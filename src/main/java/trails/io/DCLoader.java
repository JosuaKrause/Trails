package trails.io;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import jkanvas.util.Resource;
import trails.io.SQLHandler.InsertStatement;

/**
 * Loads DC data sets.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class DCLoader {

  /** The file filter. */
  private static FileFilter FILTER = new FileFilter() {

    @Override
    public boolean accept(final File file) {
      return file.isFile() && file.getName().endsWith(".csv");
    }

  };

  /** Force station creation. */
  public static boolean CREATE_STATIONS = false;

  /**
   * Fills the database with Washington-DC trips.
   * 
   * @param args No arguments.
   * @throws Exception Exception.
   */
  public static void main(final String[] args) throws Exception {
    final File stationFile = new File("src/main/resources/washington-dc/stations.txt");
    if(CREATE_STATIONS || !stationFile.exists()) {
      System.out.println("create station file");
      createStationFile(stationFile);
    }
    final Map<String, Point2D> stations = new HashMap<>();
    final CSVReader reader = new CSVReader(',', '"', false, false, true);
    for(final CSVRow row : CSVReader.readRows(Resource.getFor(stationFile), reader)) {
      try {
        final String name = row.get(0);
        final double lat = Double.parseDouble(row.get(1));
        final double lon = Double.parseDouble(row.get(2));
        stations.put(name, new Point2D.Double(lon, lat));
      } catch(final Exception e) {
        // continue
      }
    }
    fillDatabase(stations);
  }

  /**
   * Fills the data base.
   * 
   * @param stations The station map.
   * @throws Exception General Exception.
   */
  private static void fillDatabase(final Map<String, Point2D> stations) throws Exception {
    final SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/YYYY HH:mm");
    final File folder = new File("src/main/resources/washington-dc/");
    final CSVReader reader = new CSVReader(',', '"', true, false, true);
    try (SQLHandler sql = new SQLHandler("dc_trips")) {
      sql.truncateTable();
      long num = 0;
      for(final File f : folder.listFiles(FILTER)) {
        System.out.println("reading " + f);
        final Trip t = new Trip();
        t.setVehicle(0);
        try (InsertStatement is = sql.beginSection()) {
          for(final CSVRow row : CSVReader.readRows(Resource.getFor(f), reader)) {
            final Point2D from = stations.get(row.get("Start Station"));
            final Point2D to = stations.get(row.get("End Station"));
            if(from == null || to == null) {
              continue;
            }
            try {
              final Date time = fmt.parse(row.get("Start time"));
              final long start = time.getTime();
              final String[] duration = row.get("Duration").split(" ");
              final int hour = Integer.parseInt(
                  duration[0].substring(0, duration[0].length() - 1));
              final int min = Integer.parseInt(
                  duration[1].substring(0, duration[1].length() - 1));
              final int sec = Integer.parseInt(
                  duration[2].substring(0, duration[2].length() - 1));
              final long end = start + ((hour * 60L + min) * 60L + sec) * 1000L;
              t.set(num++, from.getY(), from.getX(), start, to.getY(), to.getX(), end);
              is.insert(t);
            } catch(final Exception e) {
              // continue...
              System.err.println(row.toString());
            }
          }
        }
      }
      System.out.println("number of trips: " + num);
    }
  }

  /**
   * Creates a new station file. This does not contain locations.
   * 
   * @param out The file.
   * @throws IOException I/O Exception.
   */
  public static void createStationFile(final File out) throws IOException {
    final File folder = new File("src/main/resources/washington-dc/");
    final Set<String> stations = new HashSet<>();
    final CSVReader reader = new CSVReader(',', '"', true, false, true);
    for(final File f : folder.listFiles(FILTER)) {
      System.out.println("reading " + f);
      for(final CSVRow row : CSVReader.readRows(Resource.getFor(f), reader)) {
        stations.add(row.get("Start Station"));
        stations.add(row.get("End Station"));
      }
    }
    System.out.println("writing stations to " + out);
    try (PrintWriter o = new PrintWriter(out)) {
      for(final String s : stations) {
        if(s == null) {
          continue;
        }
        final String t = s.trim();
        if(!t.isEmpty()) {
          o.println(t);
        }
      }
    }
  }

}
