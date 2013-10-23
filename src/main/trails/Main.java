package trails;

import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JFrame;

import jkanvas.Canvas;
import jkanvas.animation.AnimatedPainter;
import jkanvas.animation.AnimationAction;
import jkanvas.animation.AnimationTiming;
import jkanvas.examples.ExampleUtil;
import jkanvas.util.Interpolator;
import jkanvas.util.VecUtil;

/**
 * Starts the main project.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Main {

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

  /**
   * Starts the main application.
   * 
   * @param args No arguments.
   */
  public static void main(final String[] args) {
    final AnimatedPainter p = new AnimatedPainter() {

      private long time = 0;

      @Override
      protected long getTime() {
        if(isStopped()) return time;
        final long t = time;
        time += 10;
        return t;
      }

    };
    p.setFramerate(60);
    final Canvas c = new Canvas(p, true, 1024, 768);
    final JFrame frame = new JFrame("Trails") {

      @Override
      public void dispose() {
        c.dispose();
        super.dispose();
      }

    };
    final TrailRenderpass trails = new TrailRenderpass(p, 500, 500);
    for(int i = 0; i < 100; ++i) {
      final Point2D pos = nextPosition(trails.getWidth(), trails.getHeight());
      final Particle part = new Particle(pos.getX(), pos.getY(),
          3.0 + Math.random() * 3.0);
      trails.add(part);
      nextDestination(trails.getWidth(), trails.getHeight(), part);
    }
    ExampleUtil.setupCanvas(frame, c, p, true, true, true, true);
    frame.setLayout(new BorderLayout());
    frame.remove(c);
    frame.add(c, BorderLayout.CENTER);
    frame.add(new ControlPanel(new Controller(c)), BorderLayout.WEST);
    frame.pack();
    p.addPass(trails);
    c.reset();
  }

  /**
   * Sends the particle to its next position.
   * 
   * @param w The width of the field.
   * @param h The height of the field.
   * @param p The particle.
   */
  protected static void nextDestination(final double w, final double h, final Particle p) {
    final ThreadLocalRandom r = ThreadLocalRandom.current();
    final Point2D pos = new Point2D.Double(r.nextGaussian() * 40, r.nextGaussian() * 40);
    p.setPosition(nextPosition(w, h));
    final AnimationTiming timing = new AnimationTiming(Interpolator.QUAD_IN_OUT,
        (long) (Math.abs(2000.0 + r.nextGaussian() * 2000.0)));
    final Point2D end = VecUtil.addVec(p.getPredict(), pos);
    p.startAnimationTo(end, timing, new AnimationAction() {

      @Override
      public void animationFinished() {
        nextDestination(w, h, p);
      }

    });
  }

}
