package trails.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jkanvas.util.Resource;

public class TripManager implements AutoCloseable {

  protected static final boolean SCAN_ALL = false;

  protected static int blockTrips = 10000;

  private class TripBlock {

    private final long offset;

    private final long trips;

    private long startTime = -1;

    private long endTime = -1;

    private MappedByteBuffer buffer;

    public TripBlock(final long from, final long trips) throws IOException {
      offset = from;
      this.trips = Math.min(trips, blockTrips);
      buffer = null;
    }

    private void ensureBuffer() throws IOException {
      ensureOpen();
      if(buffer == null) {
        buffer = fc.map(FileChannel.MapMode.READ_ONLY,
            offset * Trip.byteSize(), trips * Trip.byteSize());
      }
    }

    public long getTrips() {
      return trips;
    }

    public long getStartTime() throws IOException {
      if(startTime < 0) {
        ensureBuffer();
        final Trip trip = new Trip();
        Trip.seek(buffer, offset, offset);
        for(int i = 0; i < trips; ++i) {
          trip.read(buffer, offset + i);
          if(trip.isValid()) {
            break;
          }
        }
        if(!trip.isValid()) {
          startTime = -1;
        } else {
          startTime = trip.getPickupTime();
        }
      }
      return startTime;
    }

    public long getEndTime() throws IOException {
      if(endTime < 0) {
        ensureBuffer();
        final Trip trip = new Trip();
        for(long i = trips - 1; i >= 0; --i) {
          Trip.seek(buffer, offset + i, offset);
          trip.read(buffer, offset + i);
          if(trip.isValid()) {
            break;
          }
        }
        if(!trip.isValid()) {
          endTime = -1;
        } else {
          endTime = trip.getPickupTime();
        }
      }
      return endTime;
    }

    public void read(final List<Trip> list, final long fromTime, final long toTime)
        throws IOException {
      ensureBuffer();
      Trip.seek(buffer, offset, offset);
      if(SCAN_ALL) {
        for(int i = 0; i < trips; ++i) {
          final Trip trip = new Trip();
          trip.read(buffer, i + offset);
          if(!trip.isValid()) {
            System.err.println("invalid entry");
            continue;
          }
          final long time = trip.getPickupTime();
          if(time <= toTime && time >= fromTime) {
            list.add(trip);
          }
        }
        return;
      }
      Trip cur = new Trip();
      for(int i = 0; i < trips; ++i) {
        cur.read(buffer, i + offset);
        if(!cur.isValid()) {
          System.err.println("invalid entry");
          continue;
        }
        final long time = cur.getPickupTime();
        if(time > toTime) return;
        if(time >= fromTime) {
          list.add(cur);
          cur = new Trip();
        }
      }
    }

    public void read(final Trip trip, final long index) throws IOException {
      ensureBuffer();
      Trip.seek(buffer, index, offset);
      trip.read(buffer, index);
    }

  } // TripBlock

  private final long size;
  private RandomAccessFile raf;
  private final List<TripBlock> blocks;
  protected final FileChannel fc;

  public TripManager(final Resource r) throws IOException {
    raf = new RandomAccessFile(r.directFile(), "r");
    fc = raf.getChannel();
    size = fc.size() / Trip.byteSize();
    blocks = new ArrayList<>();
    long offset = 0;
    while(offset < size) {
      final TripBlock block = new TripBlock(offset, size - offset);
      offset += block.getTrips();
      blocks.add(block);
    }
  }

  private void ensureOpen() {
    if(raf == null) throw new IllegalStateException("already closed");
  }

  public List<Trip> read(final long fromTime, final long toTime) throws IOException {
    if(fromTime > toTime) throw new IllegalArgumentException(fromTime + " > " + toTime);
    if(SCAN_ALL) {
      final List<Trip> list = new ArrayList<>();
      for(final TripBlock block : blocks) {
        block.read(list, fromTime, toTime);
      }
      return list;
    }
    int blockIndex = 0;
    for(;;) {
      if(blockIndex >= blocks.size()) return Collections.emptyList();
      final TripBlock block = blocks.get(blockIndex);
      final long startTime = block.getStartTime();
      if(startTime >= fromTime) {
        break;
      }
      final long endTime = block.getEndTime();
      if(endTime >= fromTime) {
        break;
      }
      ++blockIndex;
    }
    final List<Trip> list = new ArrayList<>();
    while(blockIndex < blocks.size()) {
      final TripBlock block = blocks.get(blockIndex);
      if(block.getStartTime() > toTime) {
        break;
      }
      block.read(list, fromTime, toTime);
      ++blockIndex;
    }
    return list;
  }

  public void read(final Trip trip, final long index) throws IOException {
    final int blockIndex = (int) (index / blockTrips);
    final TripBlock block = blocks.get(blockIndex);
    block.read(trip, index);
  }

  public long getStartTime() throws IOException {
    if(blocks.size() <= 0) return -1L;
    return blocks.get(0).getStartTime();
  }

  public long getEndTime() throws IOException {
    if(blocks.size() <= 0) return -1L;
    return blocks.get(blocks.size() - 1).getEndTime();
  }

  @Override
  public void close() throws IOException {
    if(raf != null) {
      fc.close();
      raf.close();
      raf = null;
    }
  }

  public static TripManager getManager(final Resource bin, final Resource origin)
      throws IOException {
    if(!bin.hasContent()) {
      final CSVTripLoader loader = new CSVTripLoader(origin);
      loader.loadTrips(bin.directFile(), 0L);
    }
    try (TripSorter sorter = new TripSorter(bin.directFile())) {
      sorter.sort();
    } catch(final Exception e) {
      throw new IOException(e);
    }
    return new TripManager(bin);
  }

}
