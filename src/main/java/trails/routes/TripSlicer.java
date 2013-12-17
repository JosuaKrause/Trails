package trails.routes;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import trails.BarChartRenderpass;
import trails.io.Trip;
import trails.io.TripManager;
import trails.particels.Particle;
import trails.particels.ParticleProvider;

/**
 * Slices trips into time frames.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class TripSlicer extends TimeSlicer {

  /** The underlying trip manager. */
  private final TripManager mng;
  /** The current time. */
  private long curTime;
  /** The current index. */
  private long curIndex;
  /** The bar chart. */
  protected final BarChartRenderpass bc;

  /**
   * Creates a new trip slicer.
   * 
   * @param mng The trip manager.
   * @param bc The bar chart showing number of possible trips.
   * @throws IOException I/O Exception.
   */
  public TripSlicer(final TripManager mng, final BarChartRenderpass bc)
      throws IOException {
    this.bc = Objects.requireNonNull(bc);
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
    onChange();
  }

  /**
   * Fills the whole bar chart.
   * 
   * @throws IOException I/O Exception.
   */
  protected void fillBarChart() throws IOException {
    synchronized(bc) {
      needUpdate = false;
      long time = previousTime(curTime);
      for(int i = 0; i < bc.size(); ++i) {
        final long startInterval = time + getIntervalFrom();
        final long endInterval = time + getIntervalTo();
        // red
        final int cA = mng.count(startInterval, endInterval, 0);
        bc.set(i, true, cA);
        // blue
        final int cB = mng.count(startInterval, endInterval, 1);
        bc.set(i, false, cB);
        time = advanceTime(time);
      }
    }
  }

  /**
   * Fills the last slot and advances the bar chart.
   * 
   * @param time The time of the second slot (the current time).
   * @throws IOException I/O Exception.
   */
  private void fillLastSlot(final long time) throws IOException {
    synchronized(bc) {
      long t = time;
      for(int i = 0; i < bc.size() - 1; ++i) {
        t = advanceTime(t);
      }
      final long startInterval = t + getIntervalFrom();
      final long endInterval = t + getIntervalTo();
      // red
      final int cA = mng.count(startInterval, endInterval, 0);
      bc.set(0, true, cA);
      // blue
      final int cB = mng.count(startInterval, endInterval, 1);
      bc.set(0, false, cB);
      bc.shift(1);
    }
  }

  /** The update loop. */
  private final Runnable updateLoop = new Runnable() {

    @Override
    public void run() {
      while(!Thread.currentThread().isInterrupted()) {
        if(needUpdate) {
          try {
            fillBarChart();
          } catch(final IOException e) {
            e.printStackTrace();
          }
        }
        try {
          synchronized(bc) {
            bc.wait(100);
          }
        } catch(final InterruptedException e) {
          return;
        }
      }
    }

  };

  /** The current updating thread. */
  private Thread updater;
  /** Whether the bar chart needs an update. */
  protected volatile boolean needUpdate = false;

  @Override
  protected void onChange() {
    if(updater == null || !updater.isAlive()) {
      synchronized(bc) {
        if(updater != null) {
          updater.interrupt();
        }
        updater = new Thread(updateLoop);
        updater.setDaemon(true);
        updater.start();
      }
    } else if(needUpdate) return;
    synchronized(bc) {
      needUpdate = true;
      bc.notifyAll();
    }
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
  private static boolean SKIP_GAPS = true;

  /**
   * Setter.
   * 
   * @param skipGaps Whether to skip gaps.
   */
  public static final void setSkipGaps(final boolean skipGaps) {
    SKIP_GAPS = skipGaps;
  }

  /**
   * Getter.
   * 
   * @return Whether to skip gaps.
   */
  public static final boolean isSkippingGaps() {
    return SKIP_GAPS;
  }

  /**
   * A trip for aggregation.
   * 
   * @author Joschi <josua.krause@gmail.com>
   */
  private static final class Aggregated {

    /** The start position. */
    public final Point2D from;
    /** The end position. */
    public final Point2D to;
    /** The duration in slices. */
    public final int slices;
    /** Vehicle. */
    public final long vehicle;

    /**
     * Creates a trip for aggregation.
     * 
     * @param from The start position.
     * @param to The end position.
     * @param slices The duration in slices.
     * @param vehicle The vehicle.
     */
    public Aggregated(final Point2D from, final Point2D to,
        final int slices, final long vehicle) {
      this.from = Objects.requireNonNull(from);
      this.to = Objects.requireNonNull(to);
      this.vehicle = vehicle;
      this.slices = slices;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + from.hashCode();
      result = prime * result + slices;
      result = prime * result + to.hashCode();
      result = prime * result + ((Long) vehicle).hashCode();
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if(this == obj) return true;
      if(!(obj instanceof Aggregated)) return false;
      final Aggregated other = (Aggregated) obj;
      if(!from.equals(other.from)) return false;
      if(!to.equals(other.to)) return false;
      if(vehicle != other.vehicle) return false;
      return slices == other.slices;
    }

  } // Aggregated

  @Override
  public void timeSlice(final ParticleProvider provider, final int width, final int height) {
    if(curTime < 0) throw new IllegalStateException("no start");
    int skipped = -1;
    final SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd E HH:mm:ss ");
    try {
      int no;
      do {
        ++skipped;
        final long startInterval = curTime + getIntervalFrom();
        final long endInterval = curTime + getIntervalTo();
        final List<Trip> list = mng.read(curIndex, startInterval, endInterval);
        final Map<Aggregated, Integer> journeys = new HashMap<>();
        for(final Trip t : list) {
          final int slices = getNumberOfSlices(curTime, t.getDropoffTime());
          final Point2D from = new Point2D.Double(getX(t.getPickupLon(), width),
              getY(t.getPickupLat(), height));
          final Point2D to = new Point2D.Double(getX(t.getDropoffLon(), width),
              getY(t.getDropoffLat(), height));
          final Aggregated agg = new Aggregated(from, to, slices, t.getVehicle());
          Integer num = journeys.get(agg);
          if(num == null) {
            num = 0;
          }
          journeys.put(agg, num + 1);
        }
        for(final Entry<Aggregated, Integer> e : journeys.entrySet()) {
          final Aggregated agg = e.getKey();
          final int num = e.getValue();
          if(num < getThreshold()) {
            continue;
          }
          final int col = agg.vehicle == 0 ? Particle.RED : Particle.BLUE;
          provider.startPath(agg.from.getX(), agg.from.getY(),
              agg.to, agg.slices, Math.log(num) + 1.0, col);
        }
        no = list.size();
        if(no != 0) {
          curIndex = list.get(list.size() - 1).getIndex() + 1L;
        }
        final long lastTime = curTime;
        curTime = advanceTime(curTime);
        if(curTime < lastTime) {
          curIndex = 0L;
          System.out.println("full cycle!");
        }
        System.out.println("trips: " + no);
        setInfoText(
            fmt.format(new Date(startInterval)), fmt.format(new Date(endInterval)));
      } while(SKIP_GAPS && no == 0);
      if(skipped == 0) {
        fillLastSlot(curTime);
      } else {
        fillBarChart();
      }
    } catch(final IOException io) {
      throw new IllegalStateException(io);
    }
  }

  /**
   * Advances the time.
   * 
   * @param time The time.
   * @return The next time step.
   * @throws IOException I/O Exception.
   */
  private long advanceTime(final long time) throws IOException {
    final long end = mng.getEndTime();
    final long cTime = time + getTimeSlice();
    return (cTime > end) ? mng.getStartTime() : cTime;
  }

  /**
   * Computes the previous time.
   * 
   * @param time The time.
   * @return The previous time.
   */
  private long previousTime(final long time) {
    return time - getTimeSlice();
  }

}
