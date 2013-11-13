package trails.io;

import jkanvas.io.csv.CSVRow;

public interface CSVFormat {

  boolean readTrip(Trip t, CSVRow row, long rowNo);

}
