package trails.routes;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import trails.io.Trip;
import trails.io.TripManager;
import trails.particels.ParticleProvider;
import trails.particels.TimeSlicer;

/**
 * Slices trips into time frames.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class TripSlicer implements TimeSlicer {

  /** The underlying trip manager. */
  private final TripManager mng;
  /** The size of the time slice. */
  private long timeSlice = 5L * 60L * 1000L; // 5min
  /** The current time. */
  private long curTime;
  /** The current index. */
  private long curIndex;

  /**
   * Creates a new trip slicer.
   * 
   * @param mng The trip manager.
   * @throws IOException I/O Exception.
   */
  public TripSlicer(final TripManager mng) throws IOException {
    this.mng = Objects.requireNonNull(mng);
    // ignore first slice because of garbage
    curTime = mng.getStartTime() + timeSlice;
    curIndex = 0L;
    final double l = mng.getMinLon();
    left = Double.isNaN(l) ? -74.099464 : l;
    final double r = mng.getMaxLon();
    right = Double.isNaN(r) ? -73.760262 : r;
    final double b = mng.getMinLat();
    bottom = Double.isNaN(b) ? 40.532589 : b;
    final double t = mng.getMaxLat();
    top = Double.isNaN(t) ? 40.862122 : t;
  }

  /**
   * Getter.
   * 
   * @return The size of the time slice.
   */
  public long getTimeSlice() {
    return timeSlice;
  }

  /**
   * Setter.
   * 
   * @param timeSlice The size of the time slice.
   */
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

  @Override
  public void timeSlice(final ParticleProvider provider, final int width, final int height) {
    if(curTime < 0) throw new IllegalStateException("no start");
    try {
      int no;
      do {
        final long curEnd = curTime + timeSlice - 1L;
        final List<Trip> list = mng.read(curIndex, curTime, curEnd);
        for(final Trip t : list) {
          final int slices = (int) ((t.getDropoffTime() - curTime) / timeSlice) + 1;
          final double startX = getX(t.getPickupLon(), width);
          final double startY = getY(t.getPickupLat(), height);
          final double endX = getX(t.getDropoffLon(), width);
          final double endY = getY(t.getDropoffLat(), height);
          provider.startPath(startX, startY,
              new Point2D.Double(endX, endY), slices, 2.0);
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
