package trails.io;

import java.io.IOException;
import java.util.List;

/**
 * A device independent view on trips.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public interface TripManager extends AutoCloseable {

  /**
   * Reads all trips that lie in the given time span.
   * 
   * @param startIndex A hint for the starting index. This value may be ignored.
   * @param fromTime The lowest inclusive time.
   * @param toTime The highest exclusive time.
   * @return The list containing the trips.
   * @throws IOException I/O Exception.
   */
  List<Trip> read(long startIndex, long fromTime, long toTime) throws IOException;

  /**
   * Counts all trips that lie in the given time span.
   * 
   * @param fromTime The lowest inclusive time.
   * @param toTime The highest exclusive time.
   * @param vehicle The vehicle.
   * @return The number of trips.
   * @throws IOException I/O Exception.
   */
  int count(long fromTime, long toTime, long vehicle) throws IOException;

  /**
   * Getter.
   * 
   * @return The time of the very first entry.
   * @throws IOException I/O Exception.
   */
  long getStartTime() throws IOException;

  /**
   * Getter.
   * 
   * @return The time of the very last entry.
   * @throws IOException I/O Exception.
   */
  long getEndTime() throws IOException;

  /**
   * Getter.
   * 
   * @return The smallest latitude or <code>NaN</code> if standard values should
   *         be used.
   * @throws IOException I/O Exception.
   */
  double getMinLat() throws IOException;

  /**
   * Getter.
   * 
   * @return The largest latitude or <code>NaN</code> if standard values should
   *         be used.
   * @throws IOException I/O Exception.
   */
  double getMaxLat() throws IOException;

  /**
   * Getter.
   * 
   * @return The smallest longitude or <code>NaN</code> if standard values
   *         should be used.
   * @throws IOException I/O Exception.
   */
  double getMinLon() throws IOException;

  /**
   * Getter.
   * 
   * @return The largest longitude or <code>NaN</code> if standard values should
   *         be used.
   * @throws IOException I/O Exception.
   */
  double getMaxLon() throws IOException;

}
