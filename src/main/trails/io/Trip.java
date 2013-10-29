package trails.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Trip {

  public final double pLat;

  public final double pLon;

  public final long pTime;

  public final double dLat;

  public final double dLon;

  public final long dTime;

  public Trip(final String pickupLat, final String pickupLon, final String pickupTime,
      final String dropoffLat, final String dropoffLon, final String dropoffTime) {
    pLat = parseDouble(pickupLat);
    pLon = parseDouble(pickupLon);
    dLat = parseDouble(dropoffLat);
    dLon = parseDouble(dropoffLon);
    pTime = parseDate(pickupTime);
    dTime = parseDate(dropoffTime);
  }

  private static double parseDouble(final String d) {
    try {
      return Double.parseDouble(d);
    } catch(NumberFormatException | NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** The date format. */
  private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss");

  private static long parseDate(final String d) {
    try {
      final Date date = DATE_PARSER.parse(d);
      return date.getTime();
    } catch(ParseException | NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
