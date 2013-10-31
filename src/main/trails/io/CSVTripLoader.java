package trails.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import jkanvas.util.Resource;

public class CSVTripLoader {

  private final Resource r;

  private final CSVReader reader = new CSVReader(',', '"', true, false, true);

  public CSVTripLoader(final Resource r) {
    this.r = Objects.requireNonNull(r);
  }

  public void loadTrips(final File out, final int off) throws IOException {
    final Integer[] perm = getPermutation();
    final Trip t = new Trip();
    try (RandomAccessFile outFile = new RandomAccessFile(out, "rw")) {
      outFile.setLength(Trip.byteSize() * perm.length);
      int rowNo = off;
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
        t.write(outFile);
        ++rowNo;
      }
      System.out.println(rowNo + " trips");
    }
  }

  private Integer[] getPermutation() throws IOException {
    final List<Long> times = new ArrayList<>();
    final Iterator<CSVRow> it = openFile("trip_data_1.csv");
    while(it.hasNext()) {
      final CSVRow row = it.next();
      final long date = Trip.parseDate(row.get("pickup_datetime"));
      times.add(date);
    }
    System.out.println(times.size() + " " + Runtime.getRuntime().totalMemory());
    final Integer[] perm = new Integer[times.size()];
    for(int i = 0; i < perm.length; ++i) {
      perm[i] = i;
    }
    Arrays.sort(perm, new Comparator<Integer>() {

      @Override
      public int compare(final Integer o1, final Integer o2) {
        return Long.compare(times.get(o1), times.get(o2));
      }

    });
    return perm;
  }

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

  public static void main(final String[] args) throws IOException {
    final CSVTripLoader l = new CSVTripLoader(Resource.getFor("trip_data_1.csv.zip"));
    final Resource dump = new Resource(
        (String) null, "trip_data_1.dat", (String) null, (String) null);
    l.loadTrips(dump.directFile(), 0);
    System.out.println("finished!");

    // RandomAccessFile raf = new RandomAccessFile(file, "r");
    // FileChannel fc = raf.getChannel();
    // MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0,
    // fc.size());
  }

}
