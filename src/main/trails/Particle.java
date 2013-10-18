package trails;

import static jkanvas.util.VecUtil.*;

import java.awt.geom.Point2D;

import jkanvas.animation.AnimatedPosition;
import jkanvas.animation.GenericAnimated;

public class Particle extends AnimatedPosition {

  public static double bendRatio = 0.4;

  private final double size;

  public Particle(final double x, final double y, final double size) {
    super(x, y);
    this.size = size;
  }

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
