package trails.io;

import java.io.IOException;

/**
 * Accepts trips and stores them in a permanent representation.
 * 
 * @author Joschi <josua.krause@gmail.com>
 * @param <T> The type of storage device.
 */
public interface TripAcceptor<T extends AutoCloseable> {

  /**
   * Creates a token to store the trips.
   * 
   * @return The token to store the trips.
   * @throws IOException I/O Exception.
   */
  T beginSection() throws IOException;

  /**
   * Accepts a trip.
   * 
   * @param out The storage device to store the trip in.
   * @param t The trip.
   * @param rowNo The row number. This value may be ignored.
   * @throws IOException I/O Exception.
   */
  void accept(T out, Trip t, long rowNo) throws IOException;

}
