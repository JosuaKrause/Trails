package trails.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jkanvas.util.Resource;

/**
 * Manages the trips file.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class TripManager implements AutoCloseable {

  /** Enforces sorting of the file. */
  private static final boolean ENFORCE_SORT = false;
  /** Enforces to scan all records when searching. */
  protected static final boolean SCAN_ALL = true;
  /** How many trips a block has. */
  protected static int blockTrips = 10000;

  /**
   * A block in the file.
   * 
   * @author Joschi <josua.krause@gmail.com>
   */
  private class TripBlock {

    /** The index offset for the block. */
    private final long offset;
    /** The number of trips in the block. */
    private final long trips;
    /** The time of the first entry. */
    private long startTime = -1;
    /** The time of the last entry. */
    private long endTime = -1;
    /** The memory map. */
    private MappedByteBuffer buffer;

    /**
     * Creates a block.
     * 
     * @param from The starting index.
     * @param trips The number of trips.
     */
    public TripBlock(final long from, final long trips) {
      offset = from;
      this.trips = Math.min(trips, blockTrips);
      buffer = null;
    }

    /**
     * Checks whether the given index is in this block.
     * 
     * @param index The index.
     * @return Whether the index is in this block.
     */
    public boolean contains(final long index) {
      return index >= offset && index < offset + trips;
    }

    /**
     * Ensures that the buffer exists.
     * 
     * @throws IOException I/O exception.
     */
    private void ensureBuffer() throws IOException {
      ensureOpen();
      if(buffer == null) {
        buffer = fc.map(FileChannel.MapMode.READ_ONLY,
            offset * Trip.byteSize(), trips * Trip.byteSize());
      }
    }

    /**
     * Getter.
     * 
     * @return The number of trips.
     */
    public long getTrips() {
      return trips;
    }

    /**
     * Getter.
     * 
     * @return The time of the first entry.
     * @throws IOException I/O Exception.
     */
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

    /**
     * Getter.
     * 
     * @return The time of the last entry.
     * @throws IOException I/O Exception.
     */
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

    /**
     * Reads all entries of the block matching the criterias.
     * 
     * @param list The list to fill.
     * @param startIndex The start index.
     * @param fromTime The inclusive lowest time that will be added.
     * @param toTime The inclusive highest time will be added.
     * @throws IOException I/O Exception.
     */
    public void read(final List<Trip> list, final long startIndex,
        final long fromTime, final long toTime)
        throws IOException {
      ensureBuffer();
      if(contains(startIndex)) {
        Trip.seek(buffer, startIndex, offset);
      } else {
        Trip.seek(buffer, offset, offset);
      }
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

    /**
     * Reads the trip at the given index.
     * 
     * @param trip The trip to store the info.
     * @param index The absolute index of the trip.
     * @throws IOException I/O Exception.
     */
    public void read(final Trip trip, final long index) throws IOException {
      ensureBuffer();
      Trip.seek(buffer, index, offset);
      trip.read(buffer, index);
    }

  } // TripBlock

  /** The total number of trips. */
  private final long size;
  /** The file. */
  private RandomAccessFile raf;
  /** The list of blocks. */
  private final List<TripBlock> blocks;
  /** The file channel. */
  protected final FileChannel fc;

  /**
   * Creates a new trip manager.
   * 
   * @param r The resource.
   * @throws IOException I/O Exception.
   */
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

  /** Guarantees that the file is still open. */
  protected void ensureOpen() {
    if(raf == null) throw new IllegalStateException("already closed");
  }

  /**
   * Reads all trips that lie in the given time span.
   * 
   * @param startIndex The starting index.
   * @param fromTime The lowest inclusive time.
   * @param toTime The highest inclusive time.
   * @return The list containing the trips.
   * @throws IOException I/O Exception.
   */
  public List<Trip> read(final long startIndex, final long fromTime, final long toTime)
      throws IOException {
    if(fromTime > toTime) throw new IllegalArgumentException(fromTime + " > " + toTime);
    if(SCAN_ALL) {
      final List<Trip> list = new ArrayList<>();
      for(final TripBlock block : blocks) {
        block.read(list, -1L, fromTime, toTime);
      }
      return list;
    }
    int blockIndex = 0;
    for(;;) {
      if(blockIndex >= blocks.size()) return Collections.emptyList();
      final TripBlock block = blocks.get(blockIndex);
      if(block.contains(startIndex)) {
        break;
      }
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
      block.read(list, startIndex, fromTime, toTime);
      ++blockIndex;
    }
    return list;
  }

  /**
   * Reads the trip at the given index.
   * 
   * @param trip The trip to store the result in.
   * @param index The index.
   * @throws IOException I/O Exception.
   */
  public void read(final Trip trip, final long index) throws IOException {
    for(final TripBlock block : blocks) {
      if(block.contains(index)) {
        block.read(trip, index);
        return;
      }
    }
    throw new IndexOutOfBoundsException("" + index);
  }

  /**
   * Getter.
   * 
   * @return The time of the very first entry.
   * @throws IOException I/O Exception.
   */
  public long getStartTime() throws IOException {
    if(blocks.size() <= 0) return -1L;
    return blocks.get(0).getStartTime();
  }

  /**
   * Getter.
   * 
   * @return The time of the very last entry.
   * @throws IOException I/O Exception.
   */
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

  /**
   * Constructs a trip manager. The binary file gets created, filled, and sorted
   * if not already done. This may take a while.
   * 
   * @param bin The binary file.
   * @param origin The CSV file.
   * @return The trip manager.
   * @throws IOException I/O Exception.
   */
  public static TripManager getManager(final Resource bin, final Resource origin)
      throws IOException {
    if(!bin.hasContent()) {
      final CSVTripLoader loader = new CSVTripLoader(origin);
      loader.loadTrips(bin.directFile(), 0L);
    }
    if(ENFORCE_SORT) {
      try (TripSorter sorter = new TripSorter(bin.directFile())) {
        sorter.sort();
      } catch(final Exception e) {
        throw new IOException(e);
      }
    }
    return new TripManager(bin);
  }

}
