package trails;

import static jkanvas.util.VecUtil.*;

import java.awt.geom.Point2D;

import jkanvas.animation.AnimatedPosition;
import jkanvas.animation.GenericAnimated;

/**
 * A particle.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Particle extends AnimatedPosition {

  /** The curve bend ratio. */
  public static double bendRatio = 0.4;

  /** The particle size. */
  private final double size;

  /**
   * Creates a particle at the given position.
   * 
   * @param x The x coordinate.
   * @param y The y coordinate.
   * @param size The size.
   */
  public Particle(final double x, final double y, final double size) {
    super(x, y);
    this.size = size;
  }

  /**
   * Getter.
   * 
   * @return The size of the particle.
   */
  public double getSize() {
    return size;
  }

  @Override
  protected GenericAnimated<Point2D> createAnimated(final Point2D pos) {
    return new GenericAnimated<Point2D>(pos) {

      @Override
      protected Point2D interpolate(final Point2D from, final Point2D to, final double t) {
        final Point2D diff = subVec(to, from);
        final Point2D ref = addVec(mulVec(getOrthoRight(diff), bendRatio), to);
        return interpolateBezier(from, ref, to, t);
      }

    };
  }

}
