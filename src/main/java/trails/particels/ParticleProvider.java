package trails.particels;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jkanvas.animation.AnimationAction;
import jkanvas.animation.AnimationTiming;
import jkanvas.animation.Animator;
import jkanvas.util.Interpolator;

/**
 * Provides particles to animate. The particles are managed internally.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public final class ParticleProvider {

  /** The render pass for the particles. */
  private final TrailRenderpass trails;
  /** The tick action. */
  private final AnimationAction tick;
  /** The queue of unused particles. */
  protected final Queue<PathParticle> unused;
  /** The duration of a slice. */
  private long sliceTime;

  /**
   * Creates a particle provider.
   * 
   * @param animator The animator.
   * @param trails The render pass.
   * @param slicer The slicer.
   * @param sliceTime The time to display one slice.
   */
  public ParticleProvider(final Animator animator, final TrailRenderpass trails,
      final TimeSlicer slicer, final long sliceTime) {
    Objects.requireNonNull(animator);
    Objects.requireNonNull(slicer);
    this.trails = Objects.requireNonNull(trails);
    this.sliceTime = sliceTime;
    unused = new ConcurrentLinkedQueue<>();
    final ParticleProvider thiz = this;
    tick = new AnimationAction() {

      @Override
      public void animationFinished() {
        final TrailRenderpass t = thiz.getTrailRenderpass();
        slicer.timeSlice(thiz, t.getWidth(), t.getHeight());
        thiz.cleanUpUnused();
        animator.getAnimationList().scheduleAction(thiz.getTick(), thiz.getFor(1));
      }

    };
    animator.getAnimationList().scheduleAction(tick, AnimationTiming.NO_ANIMATION);
  }

  /**
   * A path particle.
   * 
   * @author Joschi <josua.krause@gmail.com>
   */
  private class PathParticle extends Particle {

    /** Whether this particle is currently in use. */
    protected volatile boolean inUse;

    /** Creates a path particle that is not in use. */
    public PathParticle() {
      super(Double.NaN, Double.NaN, Double.NaN);
      inUse = false;
    }

    /**
     * Activates this particle and starts a path.
     * 
     * @param startX The start x coordinate.
     * @param startY The start y coordinate.
     * @param end The end position.
     * @param slices How many slices the particle needs for its trip.
     * @param size The size of the particle.
     */
    public void startPath(final double startX, final double startY,
        final Point2D end, final int slices, final double size) {
      final PathParticle thiz = this;
      inUse = true;
      setSize(size);
      setPosition(startX, startY);
      startAnimationTo(end, getFor(slices), new AnimationAction() {

        @Override
        public void animationFinished() {
          inUse = false;
          setPosition(Double.NaN, Double.NaN);
          setSize(Double.NaN);
          unused.add(thiz);
        }

      });
    }

    @Override
    public boolean shouldDraw() {
      return inUse && super.shouldDraw();
    }

  } // PathParticle

  /**
   * Getter.
   * 
   * @return The animation tick.
   */
  protected AnimationAction getTick() {
    return tick;
  }

  /**
   * Getter.
   * 
   * @return The render pass.
   */
  protected TrailRenderpass getTrailRenderpass() {
    return trails;
  }

  /** The interpolation. */
  private Interpolator interpolate = Interpolator.QUAD_IN_OUT;

  /**
   * Setter.
   * 
   * @param interpolate Sets the interpolation.
   */
  public void setInterpolator(final Interpolator interpolate) {
    this.interpolate = Objects.requireNonNull(interpolate);
  }

  /**
   * Getter.
   * 
   * @return The interpolation.
   */
  public Interpolator getInterpolator() {
    return interpolate;
  }

  /**
   * Constructs the timing for the number of slices.
   * 
   * @param slices The number of slices.
   * @return The animation timing for the given number of slices.
   */
  protected AnimationTiming getFor(final int slices) {
    return new AnimationTiming(interpolate, slices * sliceTime);
  }

  /**
   * Getter.
   * 
   * @return An unused particle.
   */
  private PathParticle getUnusedParticle() {
    final PathParticle p = unused.poll();
    if(p != null) return p;
    final PathParticle n = new PathParticle();
    trails.add(n);
    return n;
  }

  /** The maximal number of particles to blean. */
  private final int cleanLimit = 1000;

  /** Clean up unused particles. */
  protected void cleanUpUnused() {
    int i = 0;
    Particle p;
    while((p = unused.poll()) != null) {
      trails.remove(p);
      if(++i >= cleanLimit) {
        break;
      }
    }
    System.out.println("cleaned " + i + " particles");
  }

  /**
   * Getter.
   * 
   * @return The time of one slice.
   */
  public long getSliceTime() {
    return sliceTime;
  }

  /**
   * Setter.
   * 
   * @param sliceTime The time of one slice.
   */
  public void setSliceTime(final long sliceTime) {
    if(sliceTime <= 1000) throw new IllegalArgumentException("" + sliceTime);
    this.sliceTime = sliceTime;
  }

  /**
   * Start a path.
   * 
   * @param startX The x coordinate.
   * @param startY The y coordinate.
   * @param end The end position.
   * @param slices How many slices this trip takes.
   * @param size The size of the particle.
   */
  public void startPath(final double startX, final double startY,
      final Point2D end, final int slices, final double size) {
    Objects.requireNonNull(end);
    final PathParticle p = getUnusedParticle();
    p.startPath(startX, startY, end, slices, size);
  }

}
