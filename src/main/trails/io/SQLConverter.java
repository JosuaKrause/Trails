package trails.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import trails.io.SQLHandler.InsertStatement;

/**
 * Converts CSV into SQL content.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class SQLConverter {

  /**
   * Fills the database with GPS trips.
   * 
   * @param args The path to the trips.
   * @throws Exception Exception.
   */
  public static void main(final String[] args) throws Exception {
    if(args.length != 1) {
      System.err.println("need path");
      return;
    }
    final File root = new File(args[0]);
    try (SQLHandler sq = new SQLHandler("gps_trips.db")) {
      sq.truncateTable();
      scanAll(root, new FileFilter() {

        @Override
        public boolean accept(final File f) {
          return f.isDirectory() || f.getName().endsWith(".plt");
        }

      }, sq, 0L);
    }
  }

  /**
   * Scans the file-system and adds all trips.
   * 
   * @param root The root file.
   * @param filter The file filter.
   * @param accept The acceptor.
   * @param tripNo The current trip number.
   * @return The trip number after completion.
   * @throws IOException I/O Exception.
   */
  private static long scanAll(final File root, final FileFilter filter,
      final TripAcceptor<InsertStatement> accept, final long tripNo) throws IOException {
    long trip = tripNo;
    for(final File folder : root.listFiles(filter)) {
      if(folder.isDirectory()) {
        trip = scanAll(folder, filter, accept, trip);
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
                final double curLon = Double.parseDouble(row.get(1));
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
        System.out.println("trip " + trip);
      }
    }
    return trip;
  }

}
