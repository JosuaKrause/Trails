package trails.io;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import jkanvas.util.Resource;
import trails.io.SQLHandler.InsertStatement;

/**
 * Loads DC data sets.
 *
 * @author Joschi <josua.krause@gmail.com>
 */
public class NYLoader {

  /** The file filter. */
  private static FileFilter FILTER = new FileFilter() {

    @Override
    public boolean accept(final File file) {
      return file.isFile() && file.getName().endsWith(".csv");
    }

  };

  /**
   * Fills the database with New York trips.
   *
   * @param args No arguments.
   * @throws Exception Exception.
   */
  public static void main(final String[] args) throws Exception {
    fillDatabase();
  }

  /**
   * Fills the data base.
   *
   * @throws Exception General Exception.
   */
  private static void fillDatabase() throws Exception {
    final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final File folder = new File("src/main/resources/nyc/");
    final CSVReader reader = new CSVReader(',', '"', true, false, true);
    try (SQLHandler sql = new SQLHandler("ny_trips")) {
      sql.truncateTable();
      long num = 0;
      for(final File f : folder.listFiles(FILTER)) {
        System.out.println("reading " + f);
        final Trip t = new Trip();
        int count = 0;
        try (InsertStatement is = sql.beginSection()) {
          for(final CSVRow row : CSVReader.readRows(Resource.getFor(f), reader)) {
            final double x1 = Double.parseDouble(row.get("start station longitude"));
            final double y1 = Double.parseDouble(row.get("start station latitude"));
            final double x2 = Double.parseDouble(row.get("end station longitude"));
            final double y2 = Double.parseDouble(row.get("end station latitude"));
            final Point2D from = new Point2D.Double(x1, y1);
            final Point2D to = new Point2D.Double(x2, y2);
            try {
              final String st = row.get("starttime");
              Objects.requireNonNull(st);
              final Date time = fmt.parse(st);
              final long start = time.getTime();
              final String duration = row.get("tripduration");
              final long end = start + Long.parseLong(duration) * 1000L;
              t.set(num++, from.getY(), from.getX(), start, to.getY(), to.getX(), end);
              final String v = row.get("usertype");
              switch(v) {
                case "Subscriber":
                  t.setVehicle(1);
                  break;
                case "Customer":
                  t.setVehicle(0);
                  break;
                default:
                  System.err.println("unknown vehicle: " + Objects.toString(v));
                  throw new Exception();
              }
              is.insert(t);
              ++count;
            } catch(final Exception e) {
              // continue...
              System.err.println(row.toString());
            }
          }

        }
        System.out.println("#trips: " + count);
      }
      System.out.println("number of trips: " + num);
    }
  }

}
