package trails.io;

import jkanvas.io.csv.CSVRow;

/**
 * Representation of the CSV format.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public interface CSVFormat {

  /**
   * Reads a trip for the given row.
   * 
   * @param t The trip to store the data.
   * @param row The current row.
   * @param rowNo The number of total trips up to now.
   * @return Whether this trip is valid.
   */
  boolean readTrip(Trip t, CSVRow row, long rowNo);

}
