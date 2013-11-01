package trails.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A trip.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Trip {

  /** The index. */
  private long index;
  /** The pickup latitude. */
  private double pLat;
  /** The pickup longitude. */
  private double pLon;
  /** The pickup time. */
  private long pTime;
  /** The drop-off latitude. */
  private double dLat;
  /** The drop-off longitude. */
  private double dLon;
  /** The drop-off time. */
  private long dTime;

  /** Creates an invalid trip. */
  public Trip() {
    set(-1, Double.NaN, Double.NaN, -1L, Double.NaN, Double.NaN, -1L);
  }

  /**
   * Getter.
   * 
   * @return The index.
   */
  public long getIndex() {
    return index;
  }

  /**
   * Getter.
   * 
   * @return The pickup latitude.
   */
  public double getPickupLat() {
    return pLat;
  }

  /**
   * Getter.
   * 
   * @return The pickup longitude.
   */
  public double getPickupLon() {
    return pLon;
  }

  /**
   * Getter.
   * 
   * @return The drop-off latitude.
   */
  public double getDropoffLat() {
    return dLat;
  }

  /**
   * Getter.
   * 
   * @return The drop-off longitude.
   */
  public double getDropoffLon() {
    return dLon;
  }

  /**
   * Getter.
   * 
   * @return The drop-off time.
   */
  public long getDropoffTime() {
    return dTime;
  }

  /**
   * Getter.
   * 
   * @return The pickup time.
   */
  public long getPickupTime() {
    return pTime;
  }

  /**
   * Setter.
   * 
   * @param index The index.
   * @param pickupLat The pickup latitude.
   * @param pickupLon The pickup longitude.
   * @param pickupTime The pickup time.
   * @param dropoffLat The drop-off latitude.
   * @param dropoffLon The drop-off longitude.
   * @param dropoffTime The drop-off time.
   */
  public void set(final long index,
      final String pickupLat, final String pickupLon, final String pickupTime,
      final String dropoffLat, final String dropoffLon, final String dropoffTime) {
    this.index = index;
    pLat = parseDouble(pickupLat);
    pLon = parseDouble(pickupLon);
    dLat = parseDouble(dropoffLat);
    dLon = parseDouble(dropoffLon);
    pTime = parseDate(pickupTime);
    dTime = parseDate(dropoffTime);
  }

  /**
   * Setter.
   * 
   * @param index The index.
   * @param pLat The pickup latitude.
   * @param pLon The pickup longitude.
   * @param pTime The pickup time.
   * @param dLat The drop-off latitude.
   * @param dLon The drop-off longitude.
   * @param dTime The drop-off time.
   */
  public void set(final long index,
      final double pLat, final double pLon, final long pTime,
      final double dLat, final double dLon, final long dTime) {
    this.index = index;
    this.pLat = pLat;
    this.pLon = pLon;
    this.pTime = pTime;
    this.dLat = dLat;
    this.dLon = dLon;
    this.dTime = dTime;
  }

  /**
   * Setter.
   * 
   * @param index The index.
   */
  public void setIndex(final long index) {
    this.index = index;
  }

  /**
   * Getter.
   * 
   * @return Whether all values are valid.
   */
  public boolean isValid() {
    return index >= 0 && pTime >= 0 && dTime >= 0 &&
        !Double.isNaN(pLat) && !Double.isNaN(pLon) &&
        !Double.isNaN(dLat) && !Double.isNaN(dLon);
  }

  /**
   * Makes an invalid trip for the given index.
   * 
   * @param index The index.
   */
  public void setInvalid(final long index) {
    this.index = index;
    pTime = -1;
    dTime = -1;
    pLat = pLon = dLat = dLon = Double.NaN;
  }

  /**
   * Parses a double.
   * 
   * @param d The double string.
   * @return The double value.
   */
  private static double parseDouble(final String d) {
    try {
      return Double.parseDouble(d);
    } catch(NumberFormatException | NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** The date format. */
  private static final String DATE_PARSER = "yyyy-MM-dd HH:mm:ss";
  /** Whether to use a safe date computation which is not entirely correct. */
  private static final boolean USE_SAFE = true;

  /**
   * Parses a date.
   * 
   * @param d The date.
   * @return The unix time stamp.
   */
  public static long parseDate(final String d) {
    try {
      if(!USE_SAFE) {
        final Date date = new SimpleDateFormat(DATE_PARSER, Locale.US).parse(d);
        return date.getTime();
      }
      final int year = Integer.parseInt(d.substring(0, 4));
      final int month = Integer.parseInt(d.substring(5, 7));
      final int day = Integer.parseInt(d.substring(8, 10));
      final int hour = Integer.parseInt(d.substring(11, 13));
      final int min = Integer.parseInt(d.substring(14, 16));
      final int sec = Integer.parseInt(d.substring(17, 19));
      // cannot use calendar -- therefore inaccurate
      return ((((((year - 1970L) * 12L + month) * 30L + day)
      * 24L + hour) * 60L + min) * 60L + sec) * 1000L;
    } catch(NumberFormatException | ParseException | NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Writes a trip to its index position.
   * 
   * @param out The file.
   * @throws IOException I/O Exception.
   */
  public void write(final RandomAccessFile out) throws IOException {
    if(index < 0) throw new IllegalArgumentException("" + index);
    out.seek(index * byteSize());
    out.writeLong(pTime); // 8
    out.writeLong(dTime); // 8
    out.writeDouble(pLat); // 8
    out.writeDouble(pLon); // 8
    out.writeDouble(dLat); // 8
    out.writeDouble(dLon); // 8
    // total bytes: 48
  }

  /**
   * Jumps to the given index.
   * 
   * @param in The buffer.
   * @param index The index.
   * @param offset The offset of the buffer.
   */
  public static void seek(final ByteBuffer in, final long index, final long offset) {
    in.position((int) ((index - offset) * byteSize()));
  }

  /**
   * Jumps to the given index.
   * 
   * @param in The file.
   * @param index The index.
   * @throws IOException I/O Exception.
   */
  public static void seek(final RandomAccessFile in, final long index) throws IOException {
    in.seek(index * byteSize());
  }

  /**
   * Reads a trip from the current buffer position.
   * 
   * @param in The buffer.
   * @param index The index to assign.
   */
  public void read(final ByteBuffer in, final long index) {
    if(index < 0) throw new IllegalArgumentException("" + index);
    final long pTime = in.getLong(); // 8
    final long dTime = in.getLong(); // 8
    final double pLat = in.getDouble(); // 8
    final double pLon = in.getDouble(); // 8
    final double dLat = in.getDouble(); // 8
    final double dLon = in.getDouble(); // 8
    // total bytes: 48
    set(index, pLat, pLon, pTime, dLat, dLon, dTime);
  }

  /**
   * Reads a trip from the current file position.
   * 
   * @param in The file.
   * @param index The index to assign.
   * @throws IOException I/O Exception.
   */
  public void read(final RandomAccessFile in, final long index) throws IOException {
    if(index < 0) throw new IllegalArgumentException("" + index);
    final long pTime = in.readLong(); // 8
    final long dTime = in.readLong(); // 8
    final double pLat = in.readDouble(); // 8
    final double pLon = in.readDouble(); // 8
    final double dLat = in.readDouble(); // 8
    final double dLon = in.readDouble(); // 8
    // total bytes: 48
    set(index, pLat, pLon, pTime, dLat, dLon, dTime);
  }

  /**
   * Getter.
   * 
   * @return The size of one entry.
   */
  public static long byteSize() {
    return 6 * 8;
  }

}
