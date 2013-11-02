package trails.routes;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import trails.io.Trip;
import trails.io.TripManager;
import trails.particels.ParticleProvider;
import trails.particels.TimeSlicer;

public class TripSlicer implements TimeSlicer {

  private final TripManager mng;

  private long timeSlice = 15L * 60L * 1000L; // 15min

  private long curTime;

  private long curIndex;

  public TripSlicer(final TripManager mng) throws IOException {
    this.mng = Objects.requireNonNull(mng);
    curTime = mng.getStartTime();
    curIndex = 0L;
  }

  public long getTimeSlice() {
    return timeSlice;
  }

  public void setTimeSlice(final long timeSlice) {
    if(timeSlice < 1000L) throw new IllegalArgumentException("" + timeSlice);
    this.timeSlice = timeSlice;
  }

  private static double left = -74.099464;

  private static double right = -73.760262;

  private static double bottom = 40.532589;

  private static double top = 40.862122;

  private static double getX(final double lon, final int width) {
    return (lon - left) / (right - left) * width;
  }

  private static double getY(final double lat, final int height) {
    return (lat - top) / (bottom - top) * height;
  }

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
