package trails.io;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import trails.io.SQLHandler.InsertStatement;

/**
 * Handles inserting and reading for the database.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class SQLHandler implements TripManager, TripAcceptor<InsertStatement> {

  /** The connection or <code>null</code> if already closed. */
  private Connection connection;

  /**
   * Creates a new database handler.
   * 
   * @param db The database name.
   * @throws SQLException SQL Exception.
   * @throws Exception Exception.
   */
  public SQLHandler(final String db) throws Exception {
    final String url = "jdbc:mysql://localhost:8889/" + db;
    final String user = "root";
    final String password = "root";
    // Table: trips
    // Columns: start_lat, start_lon, end_lat, end_lon,
    // [start_time], end_time, vehicle
    connection = DriverManager.getConnection(url, user, password);
    System.out.println(connection.getMetaData().getURL());
  }

  /** Ensures that the connection is still open. */
  private void ensureConnection() {
    Objects.requireNonNull(connection);
  }

  /** Notifies changes to the database. */
  private void onChange() {
    startTime = -1L;
    endTime = -1L;
    minLat = Double.NaN;
    maxLat = Double.NaN;
    minLon = Double.NaN;
    maxLon = Double.NaN;
  }

  /**
   * Queries the database.
   * 
   * @param query The query.
   * @return The result of the query.
   * @throws SQLException SQL Exception.
   */
  private ResultSet query(final String query) throws SQLException {
    ensureConnection();
    final Statement statement = connection.createStatement();
    statement.setQueryTimeout(30);
    // System.out.println(query);
    return statement.executeQuery(query);
  }

  /** Whether to use the simple range query. */
  private static final boolean EASY_QUERY = true;

  @Override
  public int count(final long fromTime, final long toTime) throws IOException {
    if(!EASY_QUERY) throw new IllegalStateException("must be in easy query mode");
    final String query = "SELECT COUNT(*) AS count FROM trips "
        + "WHERE start_time >= " + fromTime + " AND start_time < " + toTime;
    try {
      final ResultSet res = query(query);
      if(!res.next()) throw new IOException("no records");
      final int val = res.getInt("count");
      if(res.next()) throw new IOException("too much records");
      res.close();
      return val;
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<Trip> read(final long startIndex, final long fromTime, final long toTime)
      throws IOException {
    if(EASY_QUERY) {
      final String query = "SELECT * FROM trips "
          + "WHERE start_time >= " + fromTime + " AND start_time < " + toTime;
      try {
        final ResultSet res = query(query);
        final List<Trip> list = new ArrayList<>();
        while(res.next()) {
          final Trip t = new Trip();
          t.set(-1, res.getDouble("start_lat"), res.getDouble("start_lon"),
              res.getLong("start_time"), res.getDouble("end_lat"),
              res.getDouble("end_lon"), res.getLong("end_time"));
          t.setVehicle(res.getLong("vehicle"));
          list.add(t);
        }
        res.close();
        return list;
      } catch(final SQLException e) {
        throw new IOException(e);
      }
    }
    // only useful for GPS tracks
    final String where = "start_time >= " + fromTime + " AND start_time < " + toTime;
    final String start = "SELECT MIN(start_time) AS start_time, vehicle, start_lat, start_lon "
        + "FROM trips WHERE " + where + " GROUP BY vehicle";
    final String end = "SELECT MAX(end_time) AS end_time, vehicle, end_lat, end_lon "
        + "FROM trips WHERE " + where + " GROUP BY vehicle";
    final String q = "SELECT start.start_time AS start_time, start.vehicle AS vehicle, "
        + "start.start_lat AS start_lat, start.start_lon AS start_lon, "
        + "end.end_time AS end_time, end.end_lat AS end_lat, end.end_lon AS end_lon "
        + "FROM (" + start + ") AS start, (" + end
        + ") AS end WHERE start.vehicle = end.vehicle";
    try {
      final ResultSet res = query(q);
      final List<Trip> list = new ArrayList<>();
      while(res.next()) {
        final Trip t = new Trip();
        t.set(-1, res.getDouble("start_lat"), res.getDouble("start_lon"),
            res.getLong("start_time"), res.getDouble("end_lat"),
            res.getDouble("end_lon"), res.getLong("end_time"));
        t.setVehicle(res.getLong("vehicle"));
        list.add(t);
      }
      res.close();
      return list;
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  /** The cached start time. */
  private long startTime = -1L;

  @Override
  public long getStartTime() throws IOException {
    if(startTime < 0) {
      try {
        final ResultSet res = query("SELECT MIN(start_time) AS start_time FROM trips");
        if(!res.next()) throw new IOException("no records");
        final long startTime = res.getLong("start_time");
        if(res.next()) throw new IOException("too much records");
        res.close();
        this.startTime = startTime;
      } catch(final SQLException e) {
        throw new IOException(e);
      }
    }
    return startTime;
  }

  /** The cached end time. */
  private long endTime = -1L;

  @Override
  public long getEndTime() throws IOException {
    if(endTime < 0) {
      try {
        final ResultSet res = query("SELECT MAX(end_time) AS end_time FROM trips");
        if(!res.next()) throw new IOException("no records");
        final long endTime = res.getLong("end_time");
        if(res.next()) throw new IOException("too much records");
        res.close();
        this.endTime = endTime;
      } catch(final SQLException e) {
        throw new IOException(e);
      }
    }
    return endTime;
  }

  /**
   * Gets a single aggregated value.
   * 
   * @param aggregate The aggregation function.
   * @param name The name of the column.
   * @return The value.
   * @throws IOException I/O Exception.
   */
  private double getValue(final String aggregate, final String name) throws IOException {
    try {
      final ResultSet res = query(
          "SELECT " + aggregate + "(" + name + ") AS " + name + " FROM trips");
      if(!res.next()) throw new IOException("no records");
      final double val = res.getDouble(name);
      if(res.next()) throw new IOException("too much records");
      res.close();
      return val;
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  /** The cached minimal latitude. */
  private double minLat = Double.NaN;
  /** The cached maximal latitude. */
  private double maxLat = Double.NaN;
  /** The cached minimal longitude. */
  private double minLon = Double.NaN;
  /** The cached maximal longitude. */
  private double maxLon = Double.NaN;

  @Override
  public double getMinLat() throws IOException {
    if(Double.isNaN(minLat)) {
      final double sLat = getValue("MIN", "start_lat");
      final double eLat = getValue("MIN", "end_lat");
      minLat = Math.min(sLat, eLat);
    }
    return minLat;
  }

  @Override
  public double getMaxLat() throws IOException {
    if(Double.isNaN(maxLat)) {
      final double sLat = getValue("MAX", "start_lat");
      final double eLat = getValue("MAX", "end_lat");
      maxLat = Math.max(sLat, eLat);
    }
    return maxLat;
  }

  @Override
  public double getMinLon() throws IOException {
    if(Double.isNaN(minLon)) {
      final double sLon = getValue("MIN", "start_lon");
      final double eLon = getValue("MIN", "end_lon");
      minLon = Math.min(sLon, eLon);
    }
    return minLon;
  }

  @Override
  public double getMaxLon() throws IOException {
    if(Double.isNaN(maxLon)) {
      final double sLon = getValue("MAX", "start_lon");
      final double eLon = getValue("MAX", "end_lon");
      maxLon = Math.max(sLon, eLon);
    }
    return maxLon;
  }

  /**
   * Truncates the table.
   * 
   * @throws SQLException SQL Exception.
   */
  public void truncateTable() throws SQLException {
    final Statement stmt = connection.createStatement();
    // stmt.executeUpdate("TRUNCATE TABLE trips");
    stmt.executeUpdate("DROP TABLE IF EXISTS trips");
    final String create = "CREATE TABLE trips ("
        + " start_time bigint NOT NULL,"
        + " id int NOT NULL AUTO_INCREMENT,"
        + " end_time bigint NOT NULL,"
        + " vehicle bigint NOT NULL,"
        + " start_lat double NOT NULL,"
        + " start_lon double NOT NULL,"
        + " end_lat double NOT NULL,"
        + " end_lon double NOT NULL,"
        + " PRIMARY KEY (start_time,id)"
        + ") ENGINE=MyISAM;";
    System.out.println(create);
    stmt.executeUpdate(create);
    stmt.close();
    onChange();
  }

  /**
   * Deletes all trips from the given vehicle.
   * 
   * @param vehicle The vehicle.
   * @throws SQLException SQL-Exception.
   */
  public void deleteVehicle(final long vehicle) throws SQLException {
    final Statement stmt = connection.createStatement();
    final int num = stmt.executeUpdate("DELETE FROM trips WHERE vehicle = " + vehicle);
    System.out.println(num + " rows deleted for vehicle = " + vehicle);
    stmt.close();
    onChange();
  }

  @Override
  public void removeVehicle(final long vehicle) throws IOException {
    try {
      deleteVehicle(vehicle);
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public InsertStatement beginSection() throws IOException {
    ensureConnection();
    try {
      return new InsertStatement(connection);
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  /**
   * Can insert trips to the database.
   * 
   * @author Joschi <josua.krause@gmail.com>
   */
  static final class InsertStatement implements AutoCloseable {

    /** The statement. */
    private final PreparedStatement stmt;
    /** The connection. */
    private final Connection conn;

    /**
     * Enables to insert to the database.
     * 
     * @param conn The connection.
     * @throws SQLException SQL Exception.
     */
    public InsertStatement(final Connection conn) throws SQLException {
      this.conn = conn;
      stmt = conn.prepareStatement("INSERT INTO trips "
          + "(start_time, vehicle, end_time, start_lat, start_lon, end_lat, end_lon) "
          + "VALUES(?, ?, ?, ?, ?, ?, ?)");
    }

    /** The vehicle number. */
    private long vehicle = 0;

    /**
     * Inserts the given trip.
     * 
     * @param t The trip.
     * @throws SQLException SQL Exception.
     */
    public void insert(final Trip t) throws SQLException {
      long v;
      if(t.hasVehicle()) {
        v = t.getVehicle();
        if(v > vehicle) {
          vehicle = v + 1;
        }
      } else {
        v = vehicle++;
      }
      stmt.setLong(1, t.getPickupTime());
      stmt.setLong(2, v);
      stmt.setLong(3, t.getDropoffTime());
      stmt.setDouble(4, t.getPickupLat());
      stmt.setDouble(5, t.getPickupLon());
      stmt.setDouble(6, t.getDropoffLat());
      stmt.setDouble(7, t.getDropoffLon());
      stmt.executeUpdate();
    }

    @Override
    public void close() throws Exception {
      stmt.close();
      if(!conn.getAutoCommit()) {
        conn.commit();
      }
    }

  } // InsertStatement

  @Override
  public void accept(final InsertStatement out, final Trip t, final long rowNo)
      throws IOException {
    try {
      out.insert(t);
    } catch(final SQLException e) {
      throw new IOException(e);
    } finally {
      onChange();
    }
  }

  @Override
  public void close() throws Exception {
    if(connection != null) {
      connection.close();
      connection = null;
    }
  }

  /**
   * Fills the database with GPS trips.
   * 
   * @param args The path to the trips.
   * @throws Exception Exception.
   */
  public static void main(final String[] args) throws Exception {
    try (SQLHandler sq = new SQLHandler("gps_trips")) {
      System.out.println(print(sq.query("SELECT MIN(start_time) AS start_time, vehicle, start_lat, start_lon FROM trips WHERE start_time >= 3433892584000 AND start_time < 3433892883999 GROUP BY vehicle")));
      System.out.println(print(sq.query("SELECT MAX(end_time) AS end_time, vehicle, end_lat, end_lon FROM trips WHERE start_time >= 3433892584000 AND start_time < 3433892883999 GROUP BY vehicle")));
      System.out.println(print(sq.query("SELECT start.start_time AS start_time, start.vehicle AS vehicle, start.start_lat AS start_lat, start.start_lon AS start_lon, end.end_time AS end_time, end.end_lat AS end_lat, end.end_lon AS end_lon FROM (SELECT MIN(start_time) AS start_time, vehicle, start_lat, start_lon FROM trips WHERE start_time >= 3433892584000 AND start_time < 3433892883999 GROUP BY vehicle) AS start, (SELECT MAX(end_time) AS end_time, vehicle, end_lat, end_lon FROM trips WHERE start_time >= 3433892584000 AND start_time < 3433892883999 GROUP BY vehicle) AS end WHERE start.vehicle = end.vehicle")));
    }
  }

  /**
   * Computes a somewhat pretty result string.
   * 
   * @param res The result set.
   * @return The string.
   * @throws Exception Exception.
   */
  private static String print(final ResultSet res) throws Exception {
    final StringBuilder sb = new StringBuilder();
    final ResultSetMetaData md = res.getMetaData();
    for(int i = 0; i < md.getColumnCount(); ++i) {
      sb.append(' ');
      sb.append(md.getColumnName(i + 1));
      sb.append(" |");
    }
    sb.append('\n');
    while(res.next()) {
      for(int i = 0; i < md.getColumnCount(); ++i) {
        sb.append(' ');
        sb.append(res.getString(i + 1));
        sb.append(" |");
      }
      sb.append('\n');
    }
    return sb.toString();
  }
}
