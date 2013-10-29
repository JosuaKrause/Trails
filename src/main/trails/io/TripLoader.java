package trails.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jkanvas.io.csv.CSVReader;
import jkanvas.io.csv.CSVRow;
import jkanvas.util.Resource;

public class TripLoader {

  private final Resource r;

  private final CSVReader reader = new CSVReader(',', '"', true, false, true);

  public TripLoader(final Resource r) {
    this.r = Objects.requireNonNull(r);
  }

  public void loadTrips(final List<Trip> trips) throws IOException {
    final Iterator<CSVRow> it = openFile("trip_data_1.csv");
    while(it.hasNext()) {
      final CSVRow row = it.next();
      try {
        trips.add(
            new Trip(
                row.get("pickup_latitude"),
                row.get("pickup_longitude"),
                row.get("pickup_datetime"),
                row.get("dropoff_latitude"),
                row.get("dropoff_longitude"),
                row.get("dropoff_datetime")
            ));
      } catch(final IllegalArgumentException e) {
        System.err.println("invalid row detected");
        e.printStackTrace();
      }
    }
    Collections.sort(trips, new Comparator<Trip>() {

      @Override
      public int compare(final Trip o1, final Trip o2) {
        return Long.compare(o1.pTime, o2.pTime);
      }

    });
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
      return CSVReader.readRows(new InputStreamReader(zip, r.getCharset()), reader);
    }
    return CSVReader.readRows(r.getFile(name).reader(), reader);
  }

  public static void main(final String[] args) throws IOException {
    final TripLoader l = new TripLoader(Resource.getFor("trip_data_1.csv.zip"));
    final List<Trip> trips = new ArrayList<>();
    l.loadTrips(trips);
  }

}
