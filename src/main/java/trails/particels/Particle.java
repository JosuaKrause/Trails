package trails.particels;

import static jkanvas.util.VecUtil.*;

import java.awt.geom.Point2D;

import jkanvas.animation.AnimatedPosition;
import jkanvas.animation.GenericAnimated;

/**
 * A particle.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Particle extends AnimatedPosition implements Revokable {

  /** The color red. */
  public static final int RED = 0;
  /** The color green. */
  public static final int GREEN = 1;
  /** The color blue. */
  public static final int BLUE = 2;

  /** The curve bend ratio. */
  public static double bendRatio = 0.4;

  /** The particle size. */
  private double size;
  /** The color. */
  private int color;

  /**
   * Creates a particle at the given position.
   * 
   * @param x The x coordinate.
   * @param y The y coordinate.
   * @param size The size.
   * @param color The color. {@link Particle#RED}, {@link Particle#GREEN}, or
   *          {@link Particle#BLUE}.
   */
  public Particle(final double x, final double y, final double size, final int color) {
    super(x, y);
    this.size = size;
    this.color = color;
  }

  /**
   * Setter.
   * 
   * @param color The color. {@link Particle#RED}, {@link Particle#GREEN}, or
   *          {@link Particle#BLUE}.
   */
  public void setColor(final int color) {
    this.color = color;
  }

  /**
   * Getter.
   * 
   * @return The color. {@link Particle#RED}, {@link Particle#GREEN}, or
   *         {@link Particle#BLUE}.
   */
  public int getColor() {
    return color;
  }

  /**
   * Setter.
   * 
   * @param size The size of the particle.
   */
  public void setSize(final double size) {
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

  /**
   * Getter.
   * 
   * @return Whether the particle should be drawn.
   */
  public boolean shouldDraw() {
    return !(Double.isNaN(size) || Double.isNaN(getX()) || Double.isNaN(getY()))
        && active;
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

  /** Whether this particle is still active. */
  private boolean active = true;

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void deactivate() {
    active = false;
  }

}
