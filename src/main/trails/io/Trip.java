package trails.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Trip {

  private long index;

  private double pLat;

  private double pLon;

  private long pTime;

  private double dLat;

  private double dLon;

  private long dTime;

  public Trip() {
    set(-1, Double.NaN, Double.NaN, -1L, Double.NaN, Double.NaN, -1L);
  }

  public void set(final int index, final String pickupLat, final String pickupLon,
      final String pickupTime,
      final String dropoffLat, final String dropoffLon, final String dropoffTime) {
    this.index = index;
    pLat = parseDouble(pickupLat);
    pLon = parseDouble(pickupLon);
    dLat = parseDouble(dropoffLat);
    dLon = parseDouble(dropoffLon);
    pTime = parseDate(pickupTime);
    dTime = parseDate(dropoffTime);
  }

  public void set(final int index, final double pLat, final double pLon,
      final long pTime,
      final double dLat, final double dLon, final long dTime) {
    this.index = index;
    this.pLat = pLat;
    this.pLon = pLon;
    this.pTime = pTime;
    this.dLat = dLat;
    this.dLon = dLon;
    this.dTime = dTime;
  }

  public boolean isValid() {
    return index >= 0 && pTime >= 0 && dTime >= 0 &&
        !Double.isNaN(pLat) && !Double.isNaN(pLon) &&
        !Double.isNaN(dLat) && !Double.isNaN(dLon);
  }

  public void setInvalid(final int index) {
    this.index = index;
    pTime = -1;
    dTime = -1;
    pLat = pLon = dLat = dLon = Double.NaN;
  }

  private static double parseDouble(final String d) {
    try {
      return Double.parseDouble(d);
    } catch(NumberFormatException | NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** The date format. */
  private static final String DATE_PARSER = "yyyy-MM-dd HH:mm:ss";

  public static long parseDate(final String d) {
    try {
      final Date date = new SimpleDateFormat(DATE_PARSER, Locale.US).parse(d);
      return date.getTime();
    } catch(ParseException | NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

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

  public void read(final RandomAccessFile in, final int index) throws IOException {
    if(index < 0) throw new IllegalArgumentException("" + index);
    in.seek(index * byteSize());
    final long pTime = in.readLong(); // 8
    final long dTime = in.readLong(); // 8
    final double pLat = in.readDouble(); // 8
    final double pLon = in.readDouble(); // 8
    final double dLat = in.readDouble(); // 8
    final double dLon = in.readDouble(); // 8
    // total bytes: 48
    set(index, pLat, pLon, pTime, dLat, dLon, dTime);
  }

  public static long byteSize() {
    return 6 * 8;
  }

}
