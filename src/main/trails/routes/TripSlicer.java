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

  private long timeSlice = 60L * 60L * 1000L; // 1hour

  private long curTime;

  public TripSlicer(final TripManager mng) throws IOException {
    this.mng = Objects.requireNonNull(mng);
    curTime = mng.getStartTime();
  }

  public long getTimeSlice() {
    return timeSlice;
  }

  public void setTimeSlice(final long timeSlice) {
    if(timeSlice < 1000L) throw new IllegalArgumentException("" + timeSlice);
    this.timeSlice = timeSlice;
  }

  @Override
  public void timeSlice(final ParticleProvider provider, final int width, final int height) {
    if(curTime < 0) throw new IllegalStateException("no start");
    final long curEnd = curTime + timeSlice - 1L;
    try {
      final List<Trip> list = mng.read(curTime, curEnd);
      System.out.println(list.size());
      for(final Trip t : list) {
        final int slices = (int) ((t.getDropoffTime() - curTime) / timeSlice) + 1;
        provider.startPath(-t.getPickupLon(), t.getPickupLat(),
            new Point2D.Double(-t.getDropoffLon(), t.getDropoffLat()), slices, 2.0);
      }
      final long end = mng.getEndTime();
      curTime = curEnd + 1L;
      if(curTime > end) {
        curTime = mng.getStartTime();
      }
    } catch(final IOException io) {
      throw new IllegalStateException(io);
    }
  }

}
