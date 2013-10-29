package trails.particels;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jkanvas.animation.AnimationAction;
import jkanvas.animation.AnimationTiming;
import jkanvas.animation.Animator;
import jkanvas.util.Interpolator;

public final class ParticleProvider {

  private final TrailRenderpass trails;
  private final AnimationAction tick;
  protected final Queue<PathParticle> unused;

  private long sliceTime;

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

  private class PathParticle extends Particle {

    protected volatile boolean inUse;

    public PathParticle() {
      super(Double.NaN, Double.NaN, Double.NaN);
      inUse = false;
    }

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

  protected AnimationAction getTick() {
    return tick;
  }

  protected TrailRenderpass getTrailRenderpass() {
    return trails;
  }

  private Interpolator interpolate = Interpolator.QUAD_IN_OUT;

  public void setInterpolator(final Interpolator interpolate) {
    this.interpolate = Objects.requireNonNull(interpolate);
  }

  public Interpolator getInterpolator() {
    return interpolate;
  }

  protected AnimationTiming getFor(final int slices) {
    return new AnimationTiming(interpolate, slices * sliceTime);
  }

  private PathParticle getUnusedParticle() {
    final PathParticle p = unused.poll();
    if(p != null) return p;
    final PathParticle n = new PathParticle();
    trails.add(n);
    return n;
  }

  private final int cleanLimit = 1000;

  protected void cleanUpUnused() {
    int i = 0;
    Particle p;
    while((p = unused.poll()) != null) {
      trails.remove(p);
      if(++i >= cleanLimit) {
        break;
      }
    }
  }

  public long getSliceTime() {
    return sliceTime;
  }

  public void setSliceTime(final long sliceTime) {
    if(sliceTime <= 0) throw new IllegalArgumentException("" + sliceTime);
    this.sliceTime = sliceTime;
  }

  public void startPath(final double startX, final double startY,
      final Point2D end, final int slices, final double size) {
    Objects.requireNonNull(end);
    final PathParticle p = getUnusedParticle();
    p.startPath(startX, startY, end, slices, size);
  }

}
