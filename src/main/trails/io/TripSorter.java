package trails.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Sorts trips. Note that the class is <em>not</em> thread safe and that when
 * the application crashes during execution the file will be corrupt with high
 * probability.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class TripSorter implements AutoCloseable {

  /** The binary trip file. */
  private final RandomAccessFile raf;

  /**
   * Creates a sorter.
   * 
   * @param file The trip file.
   * @throws IOException I/O Exception.
   */
  public TripSorter(final File file) throws IOException {
    raf = new RandomAccessFile(file, "rw");
  }

  /**
   * Checks whether the file is already sorted.
   * 
   * @return Whether the file is already sorted.
   * @throws IOException I/O Exception.
   */
  public boolean isSorted() throws IOException {
    Trip.seek(raf, 0);
    final Trip trip = new Trip();
    long time = -1;
    long index = 0;
    boolean inValid = false;
    while(raf.getFilePointer() < raf.length()) {
      trip.read(raf, index);
      ++index;
      if(!trip.isValid()) {
        inValid = true;
        continue;
      }
      if(inValid) return false; // invalids at the end
      final long t = trip.getPickupTime();
      if(t < time) return false;
      time = t;
    }
    return true;
  }

  /**
   * Reads two entries.
   * 
   * @param tmpA The first trip.
   * @param tmpB The second trip.
   * @param a The index of the first trip.
   * @param b The index of the second trip.
   * @throws IOException I/O Exception.
   */
  private void readBoth(final Trip tmpA, final Trip tmpB, final long a, final long b)
      throws IOException {
    if(tmpA.getIndex() != a) {
      Trip.seek(raf, a);
      tmpA.read(raf, a);
    }
    if(tmpB.getIndex() != b) {
      Trip.seek(raf, b);
      tmpB.read(raf, b);
    }
  }

  /**
   * Swaps two entries.
   * 
   * @param tmpA The trip storage.
   * @param tmpB The trip storage.
   * @param a The first index.
   * @param b The second index.
   * @throws IOException I/O Exception.
   */
  private void swap(final Trip tmpA, final Trip tmpB, final long a, final long b)
      throws IOException {
    readBoth(tmpA, tmpB, a, b);
    final long bi = tmpB.getIndex();
    final long ai = tmpA.getIndex();
    tmpA.setIndex(bi);
    tmpB.setIndex(ai);
    tmpA.write(raf);
    tmpB.write(raf);
  }

  /**
   * Compares two trips.
   * 
   * @param tmpA The trip storage.
   * @param tmpB The trip storage.
   * @param a The first index.
   * @param b The second index.
   * @return <code>== 0</code> if both entries are equal, <code>&gt; 0</code> if
   *         the first entry is larger, and <code>&lt; 0</code> if the second
   *         entry is larger.
   * @throws IOException I/O Exception.
   */
  private int cmp(final Trip tmpA, final Trip tmpB, final long a, final long b)
      throws IOException {
    readBoth(tmpA, tmpB, a, b);
    final boolean va = tmpA.isValid();
    final boolean vb = tmpB.isValid();
    if(!va) return vb ? 1 : 0; // invalids are larger
    else if(!vb) return -1;
    return Long.compare(tmpA.getPickupTime(), tmpB.getPickupTime());
  }

  /**
   * Sorts the file.
   * 
   * @throws IOException I/O Exception.
   */
  public void sort() throws IOException {
    System.out.println("checking if sorted");
    if(isSorted()) return;
    System.out.println("start sorting");
    final long to = raf.length() / Trip.byteSize();
    sortRange(new Trip(), new Trip(), 0L, to);
    System.out.println("finished sorting");
  }

  /**
   * Sorts a range.
   * 
   * @param tmpA Temporary trip storage.
   * @param tmpB Temporary trip storage.
   * @param from The inclusive lowest index to sort.
   * @param to The exclusive highest index to sort.
   * @throws IOException I/O Exception.
   */
  private void sortRange(final Trip tmpA, final Trip tmpB, final long from, final long to)
      throws IOException {
    System.out.println("sort " + from + " -- " + to);
    if(from >= to - 1) return; // 1 or less elements
    if(from == to - 2) { // 2 elements
      if(cmp(tmpA, tmpB, from, from + 1) > 0) {
        swap(tmpA, tmpB, from, from + 1);
      }
      return;
    }
    // more elements
    long pivot = from;
    long cur = from + 1;
    while(cur < to) {
      if(cmp(tmpA, tmpB, pivot, cur) > 0) {
        if(pivot + 1 < cur) {
          swap(tmpA, tmpB, pivot, pivot + 1); // move pivot
        }
        swap(tmpA, tmpB, pivot, cur); // put to the left
        ++pivot;
      }
      ++cur;
    }
    sortRange(tmpA, tmpB, from, pivot);
    sortRange(tmpA, tmpB, pivot, to);
  }

  @Override
  public void close() throws Exception {
    raf.close();
  }

}