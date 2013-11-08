package trails.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import jkanvas.util.Resource;

/**
 * Loads trips from a csv file.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class CSVTripLoader {

  /** The resource. */
  private final Resource r;
  /** The CSV reader. */
  private final CSVReader reader = new CSVReader(',', '"', true, false, true);

  /**
   * Creates a CSV trip loader.
   * 
   * @param r The resource. Either a folder or a zip file.
   */
  public CSVTripLoader(final Resource r) {
    this.r = Objects.requireNonNull(r);
  }

  /**
   * Loads the trips from the CSV file.
   * 
   * @param <T> The trip acceptor type.
   * @param ta The acceptor.
   * @param off The offset for row numbering.
   * @throws IOException I/O Exception.
   */
  public <T extends AutoCloseable> void loadTrips(
      final TripAcceptor<T> ta, final long off) throws IOException {
    final Trip t = new Trip();
    try (T out = ta.beginSection()) {
      long rowNo = off;
      final Iterator<CSVRow> it = openFile("trip_data_1.csv");
      while(it.hasNext()) {
        final CSVRow row = it.next();
        // read the trip
        try {
          t.set(rowNo,
              row.get("pickup_latitude"),
              row.get("pickup_longitude"),
              row.get("pickup_datetime"),
              row.get("dropoff_latitude"),
              row.get("dropoff_longitude"),
              row.get("dropoff_datetime"));
        } catch(final IllegalArgumentException e) {
          System.err.println("invalid row detected");
          e.printStackTrace();
          t.setInvalid(rowNo);
        }
        ta.accept(out, t, rowNo);
        ++rowNo;
      }
      System.out.println(rowNo + " trips");
    } catch(final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Opens the file within the resource.
   * 
   * @param name The file to open.
   * @return The CSV row iterator.
   * @throws IOException I/O Exception.
   */
  private Iterator<CSVRow> openFile(final String name) throws IOException {
    if(r.isZip()) {
      final ZipInputStream zip = new ZipInputStream(r.getURL().openStream());
      boolean found = false;
      ZipEntry cur;
      while((cur = zip.getNextEntry()) != null) {
        if(name.equals(cur.getName())) {
          found = true;
          break;
        }
        zip.closeEntry();
      }
      if(!found) {
        zip.close();
        throw new FileNotFoundException(name + " in " + r);
      }
      return CSVReader.readRows(
          new BufferedReader( // always buffer zip inputs :)
              new InputStreamReader(zip, r.getCharset())), reader);
    }
    return CSVReader.readRows(r.getFile(name).reader(), reader);
  }

  /**
   * Tests the CSV loader.
   * 
   * @param args No arguments.
   * @throws IOException I/O Exception.
   */
  public static void main(final String[] args) throws IOException {
    System.out.println("start converting");
    final CSVTripLoader l = new CSVTripLoader(Resource.getFor("trip_data_1.csv.zip"));
    final Resource dump = new Resource(
        (String) null, "trip_data_1.dat", (String) null, (String) null);
    l.loadTrips(new BinaryTripAcceptor(dump.directFile()), 0L);
    System.out.println("finished!");
  }

}
