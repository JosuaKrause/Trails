package trails.routes;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import trails.io.Trip;
import trails.io.TripManager;
import trails.particels.ParticleProvider;

/**
 * Slices trips into time frames.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class TripSlicer implements TimeSlicer {

  /** The underlying trip manager. */
  private final TripManager mng;
  /** The size of the time slice. */
  private long timeSlice = 3L * 60L * 60L * 1000L; // 1h
  /** The current time. */
  private long curTime;
  /** The current index. */
  private long curIndex;
  /** The threshold. */
  private int threshold;

  /**
   * Creates a new trip slicer.
   * 
   * @param mng The trip manager.
   * @throws IOException I/O Exception.
   */
  public TripSlicer(final TripManager mng) throws IOException {
    this.mng = Objects.requireNonNull(mng);
    curTime = mng.getStartTime();
    curIndex = 0L;
    final double l = mng.getMinLon();
    left = Double.isNaN(l) ? -74.099464 : l;
    final double r = mng.getMaxLon();
    right = Double.isNaN(r) ? -73.760262 : r;
    final double b = mng.getMinLat();
    bottom = Double.isNaN(b) ? 40.532589 : b;
    final double t = mng.getMaxLat();
    top = Double.isNaN(t) ? 40.862122 : t;
    System.out.println("lon: " + left + " lat: " + top +
        " lon: " + right + " lat: " + bottom);
  }

  @Override
  public void setThreshold(final int threshold) {
    this.threshold = threshold;
  }

  @Override
  public int getThreshold() {
    return threshold;
  }

  /**
   * Getter.
   * 
   * @return The size of the time slice.
   */
  @Override
  public long getTimeSlice() {
    return timeSlice;
  }

  /**
   * Setter.
   * 
   * @param timeSlice The size of the time slice.
   */
  @Override
  public void setTimeSlice(final long timeSlice) {
    if(timeSlice < 1000L) throw new IllegalArgumentException("" + timeSlice);
    this.timeSlice = timeSlice;
  }

  /** The leftmost longitude coordinate. */
  private final double left;
  /** The rightmost longitude coordinate. */
  private final double right;
  /** The bottom latitude coordinate. */
  private final double bottom;
  /** The top latitude coordinate. */
  private final double top;

  /**
   * Converts longitude into a x coordinate.
   * 
   * @param lon The longitude.
   * @param width The display width.
   * @return The x coordinate.
   */
  private double getX(final double lon, final int width) {
    return (lon - left) / (right - left) * width;
  }

  /**
   * Converts Latitude into a y coordinate.
   * 
   * @param lat The latitude.
   * @param height The display height.
   * @return The y coordinate.
   */
  private double getY(final double lat, final int height) {
    return (lat - top) / (bottom - top) * height;
  }

  /** Whether to skip time slices with no trips. */
  private static final boolean SKIP_GAPS = true;

  private static final class Aggregated {

    public final Point2D from;

    public final Point2D to;

    public final int slices;

    public Aggregated(final Point2D from, final Point2D to, final int slices) {
      this.from = Objects.requireNonNull(from);
      this.to = Objects.requireNonNull(to);
      this.slices = slices;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((from == null) ? 0 : from.hashCode());
      result = prime * result + slices;
      result = prime * result + ((to == null) ? 0 : to.hashCode());
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if(this == obj) return true;
      if(!(obj instanceof Aggregated)) return false;
      final Aggregated other = (Aggregated) obj;
      if(!from.equals(other.from)) return false;
      if(!to.equals(other.to)) return false;
      return slices == other.slices;
    }

  } // Aggregated

  @Override
  public void timeSlice(final ParticleProvider provider, final int width, final int height) {
    if(curTime < 0) throw new IllegalStateException("no start");
    try {
      int no;
      do {
        final long curEnd = curTime + timeSlice - 1L;
        final List<Trip> list = mng.read(curIndex, curTime, curEnd);
        final Map<Aggregated, Integer> journeys = new HashMap<>();
        for(final Trip t : list) {
          final int slices = (int) ((t.getDropoffTime() - curTime) / timeSlice) + 1;
          final Point2D from = new Point2D.Double(getX(t.getPickupLon(), width),
              getY(t.getPickupLat(), height));
          final Point2D to = new Point2D.Double(getX(t.getDropoffLon(), width),
              getY(t.getDropoffLat(), height));
          final Aggregated agg = new Aggregated(from, to, slices);
          Integer num = journeys.get(agg);
          if(num == null) {
            num = 0;
          }
          journeys.put(agg, num + 1);
        }
        for(final Entry<Aggregated, Integer> e : journeys.entrySet()) {
          final Aggregated agg = e.getKey();
          final int num = e.getValue();
          if(num < threshold) {
            continue;
          }
          provider.startPath(agg.from.getX(), agg.from.getY(),
              agg.to, agg.slices, Math.log10(num) + 1.0);
        }
        no = list.size();
        final long end = mng.getEndTime();
        curTime = curEnd + 1L;
        if(no != 0) {
          curIndex = list.get(list.size() - 1).getIndex() + 1L;
        }
        if(curTime > end) {
          curTime = mng.getStartTime();
          curIndex = 0L;
          System.out.println("full cycle!");
        }
        System.out.println("trips: " + no);
      } while(SKIP_GAPS && no == 0);
    } catch(final IOException io) {
      throw new IllegalStateException(io);
    }
  }

}
