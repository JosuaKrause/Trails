package trails.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A trip acceptor for binary files.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class BinaryTripAcceptor implements TripAcceptor<RandomAccessFile> {

  /** The file to write the output to. */
  private final File out;

  /**
   * Creates a trip acceptor.
   * 
   * @param out The file to write the output to.
   */
  public BinaryTripAcceptor(final File out) {
    this.out = out;
  }

  @Override
  public RandomAccessFile beginSection() throws IOException {
    return new RandomAccessFile(out, "rw");
  }

  @Override
  public void accept(final RandomAccessFile outFile, final Trip t, final long rowNo)
      throws IOException {
    outFile.setLength(Math.max(Trip.byteSize() * rowNo, outFile.length()));
    t.write(outFile);
  }

}
