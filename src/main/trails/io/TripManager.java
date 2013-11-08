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
   * @param toTime The highest inclusive time.
   * @return The list containing the trips.
   * @throws IOException I/O Exception.
   */
  List<Trip> read(long startIndex, long fromTime, long toTime) throws IOException;

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

}
