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

  protected static int blockSizeTrips = (int) (Integer.MAX_VALUE / Trip.byteSize());

  protected static long blockSize = blockSizeTrips * Trip.byteSize();

  private class TripBlock {

    private final long offset;

    private final int trips;

    private final int size;

    private long startTime = -1;

    private long endTime = -1;

    private MappedByteBuffer buffer;

    public TripBlock(final long from, final long size) throws IOException {
      offset = from;
      this.size = (int) Math.min(size, blockSize);
      trips = (int) (size / blockSize);
      buffer = null;
    }

    private void ensureBuffer() throws IOException {
      ensureOpen();
      if(buffer == null) {
        buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, size);
      }
    }

    public int getSize() {
      return size;
    }

    public long getStartTime() throws IOException {
      if(startTime < 0) {
        ensureBuffer();
        final Trip trip = new Trip();
        Trip.seek(buffer, 0, offset);
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
        for(int i = trips - 1; i >= 0; --i) {
          Trip.seek(buffer, i, offset);
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
      Trip cur = new Trip();
      Trip.seek(buffer, 0, offset);
      for(int i = 0; i < trips; ++i) {
        cur.read(buffer, i + offset);
        if(!cur.isValid()) {
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

    public void read(final Trip trip, final long index, final int i) throws IOException {
      Trip.seek(buffer, i, offset);
      trip.read(buffer, index);
    }

  } // TripBlock

  private final FileChannel fc;
  private final long size;
  private RandomAccessFile raf;
  private final List<TripBlock> blocks;

  public TripManager(final Resource r) throws IOException {
    raf = new RandomAccessFile(r.directFile(), "r");
    fc = raf.getChannel();
    size = fc.size();
    blocks = new ArrayList<>();
    long offset = 0;
    while(offset < size) {
      final TripBlock block = new TripBlock(offset, size - offset);
      offset += block.getSize();
      blocks.add(block);
    }
  }

  private void ensureOpen() {
    if(raf == null) throw new IllegalStateException("already closed");
  }

  public List<Trip> read(final long fromTime, final long toTime) throws IOException {
    if(fromTime > toTime) throw new IllegalArgumentException(fromTime + " > " + toTime);
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
    final int blockIndex = (int) (index / blockSize);
    final int inIndex = (int) (index % blockSize);
    final TripBlock block = blocks.get(blockIndex);
    block.read(trip, index, inIndex);
  }

  @Override
  public void close() throws IOException {
    if(raf != null) {
      fc.close();
      raf.close();
      raf = null;
    }
  }

}
