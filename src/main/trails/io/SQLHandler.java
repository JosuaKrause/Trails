package trails.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import trails.io.SQLHandler.InsertStatement;

public class SQLHandler implements TripManager, TripAcceptor<InsertStatement> {

  private Connection connection;

  public SQLHandler(final String db) throws SQLException, ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    // Table: trips
    // Columns: start_lat, start_lon, end_lat, end_lon,
    // [start_time], end_time, [vehicle]
    connection = DriverManager.getConnection("jdbc:sqlite:" + db);
    System.out.println(connection.getMetaData().getURL());
  }

  private void ensureConnection() {
    Objects.requireNonNull(connection);
  }

  private ResultSet query(final String query) throws SQLException {
    ensureConnection();
    final Statement statement = connection.createStatement();
    statement.setQueryTimeout(30);
    return statement.executeQuery(query);
  }

  @Override
  public List<Trip> read(final long startIndex, final long fromTime, final long toTime)
      throws IOException {
    final String q = "SELECT * FROM trips WHERE start_time >= "
        + fromTime + " AND start_time < " + toTime;
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

  @Override
  public long getStartTime() throws IOException {
    try {
      final ResultSet res = query("SELECT MIN(start_time) AS start_time FROM trips");
      if(!res.next()) throw new IOException("no records");
      final long startTime = res.getLong("start_time");
      if(res.next()) throw new IOException("too much records");
      res.close();
      return startTime;
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public long getEndTime() throws IOException {
    try {
      final ResultSet res = query("SELECT MAX(end_time) AS end_time FROM trips");
      if(!res.next()) throw new IOException("no records");
      final long endTime = res.getLong("end_time");
      if(res.next()) throw new IOException("too much records");
      res.close();
      return endTime;
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  public void truncateTable() throws SQLException {
    final Statement stmt = connection.createStatement();
    stmt.executeUpdate("DROP TABLE IF EXISTS trips");
    final String create = "CREATE TABLE IF NOT EXISTS trips ( "
        + "start_time INTEGER PRIMARY KEY ASC, "
        + "vehicle INTEGER, "
        + "end_time INTEGER, "
        + "start_lat REAL, "
        + "start_lon REAL, "
        + "end_lat REAL, "
        + "end_lon REAL "
        + ")";
    stmt.executeUpdate(create);
    stmt.close();
  }

  @Override
  public InsertStatement beginSection() throws IOException {
    ensureConnection();
    try {
      return new InsertStatement(connection.createStatement());
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  static final class InsertStatement implements AutoCloseable {

    private final Statement stmt;

    public InsertStatement(final Statement stmt) {
      this.stmt = stmt;
    }

    private long vehicle = 0;

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
      stmt.executeUpdate("INSERT INTO trips VALUES("
          + t.getPickupTime() + ", "
          + v + ", "
          + t.getDropoffTime() + ", "
          + t.getPickupLat() + ", "
          + t.getPickupLon() + ", "
          + t.getDropoffLat() + ", "
          + t.getDropoffLon() + " "
          + ")");
      System.out.println("trip " + t.getIndex() + " : " + v);
    }

    @Override
    public void close() throws Exception {
      stmt.close();
    }

  } // InsertStatement

  @Override
  public void accept(final InsertStatement out, final Trip t, final long rowNo)
      throws IOException {
    try {
      out.insert(t);
    } catch(final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws Exception {
    if(connection != null) {
      connection.close();
      connection = null;
    }
  }

  static double minLat = Double.NaN;
  static double maxLat = Double.NaN;
  static double minLon = Double.NaN;
  static double maxLon = Double.NaN;

  public static void main(final String[] args) throws Exception {
    if(args.length != 1) {
      System.err.println("need path");
      return;
    }
    final File root = new File(args[0]);
    try (SQLHandler sq = new SQLHandler("gps_trips.db")) {
      sq.truncateTable();
      scanAll(root, ".plt", sq, 0L);
      System.out.println("Lat " + minLat + " -- " + maxLat);
      System.out.println("Lon " + minLon + " -- " + maxLon);
    }
  }

  private static long scanAll(final File root, final String suffix,
      final TripAcceptor<InsertStatement> accept, final long tripNo) throws IOException {
    long trip = tripNo;
    for(final File folder : root.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File f) {
        return f.isDirectory() || f.getName().endsWith(suffix);
      }

    })) {
      if(folder.isDirectory()) {
        trip = scanAll(folder, suffix, accept, trip);
        continue;
      }
      System.out.println(folder.getAbsolutePath());
      try (LineNumberReader r =
          new LineNumberReader(new BufferedReader(new FileReader(folder)))) {
        for(int i = 0; i < 6; ++i) {
          r.readLine();
        }
        final long curTrip = trip;
        CSVTripLoader.loadTrips(
            CSVReader.readRows(r, new CSVReader(',', '"', false, false, true)),
            accept, new CSVFormat() {

              private boolean valid = false;

              private double lastLat;

              private double lastLon;

              private long lastTime;

              @Override
              public boolean readTrip(final Trip t, final CSVRow row, final long rowNo) {
                final double curLat = Double.parseDouble(row.get(0));
                if(Double.isNaN(maxLat) || curLat > maxLat) {
                  maxLat = curLat;
                }
                if(Double.isNaN(minLat) || curLat > minLat) {
                  minLat = curLat;
                }
                final double curLon = Double.parseDouble(row.get(1));
                if(Double.isNaN(maxLon) || curLon > maxLon) {
                  maxLon = curLon;
                }
                if(Double.isNaN(minLon) || curLon > minLon) {
                  minLon = curLon;
                }
                final long curTime = (long) (Double.parseDouble(row.get(4)) * 24 * 60 * 60 * 1000);
                if(!valid) {
                  lastLat = curLat;
                  lastLon = curLon;
                  lastTime = curTime;
                } else {
                  t.set(rowNo, lastLat, lastLon, lastTime, curLat, curLon, curTime);
                  t.setVehicle(curTrip);
                }
                valid = !valid;
                return !valid;
              }

            }, 0L);
        ++trip;
      }
    }
    return trip;
  }

}
