package trails.routes;

import java.awt.geom.Point2D;
import java.util.concurrent.ThreadLocalRandom;

import trails.particels.ParticleProvider;
import trails.particels.TimeSlicer;

public class RandomTimeSlicer implements TimeSlicer {

  /**
   * Generates a random position.
   * 
   * @param w The width of the field.
   * @param h The height of the field.
   * @return A position in the field with higher probability of being near the
   *         center.
   */
  protected static Point2D nextPosition(final double w, final double h) {
    final ThreadLocalRandom r = ThreadLocalRandom.current();
    return new Point2D.Double(w * 0.5 + r.nextGaussian() * w * 0.125,
        h * 0.5 + r.nextGaussian() * h * 0.125);
  }

  @Override
  public void timeSlice(final ParticleProvider provider, final int width, final int height) {
    for(int i = 0; i < 100; ++i) {
      final Point2D start = nextPosition(width, height);
      final Point2D end = nextPosition(width, height);
      final ThreadLocalRandom rnd = ThreadLocalRandom.current();
      final int slices = rnd.nextInt(3) + 1;
      final double size = rnd.nextDouble(3, 10);
      provider.startPath(start.getX(), start.getY(), end, slices, size);
    }
  }

}
