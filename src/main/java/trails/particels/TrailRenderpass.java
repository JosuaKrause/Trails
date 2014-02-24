package trails.particels;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import jkanvas.KanvasContext;
import jkanvas.animation.Animated;
import jkanvas.animation.Animator;
import jkanvas.painter.Renderpass;
import jkanvas.util.PaintUtil;
import jkanvas.util.SnapshotList.Snapshot;
import trails.Main;

/**
 * Renders trails.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class TrailRenderpass extends Renderpass {

  /** The strength of the particles. */
  public static float particleStrength = 0.97f;
  /** The fade between frames. */
  public static double fade = 0.95;

  /** The image. */
  private final BufferedImage img;
  /** The animator that animates the particles. */
  private final Animator animator;
  /** The private animated object that notifies a redraw. */
  private final Animated animated;
  /** The list of particles. */
  private final ParticleList<Particle> particles;
  /** Whether a redraw is necessary. Value is larger than 0. */
  protected AtomicInteger ready;
  /** Whether the render pass is running. */
  protected volatile boolean running;
  /** Whether the last redraw has been finished. */
  protected volatile boolean finishedRedraw;

  /**
   * Creates a new trail render pass.
   * 
   * @param animator The animator.
   * @param width The width of the image.
   * @param height The height of the image.
   */
  public TrailRenderpass(final Animator animator, final int width, final int height) {
    this.animator = Objects.requireNonNull(animator);
    img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    ready = new AtomicInteger();
    particles = new ParticleList<>();
    final Graphics2D g = getGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, width, height);
    g.dispose();
    // need to keep a reference so it will not be destroyed
    animated = new Animated() {

      @Override
      public boolean animate(final long currentTime) {
        if(!running) return false;
        step();
        return true;
      }

    };
    animator.getAnimationList().addAnimated(animated);
    running = true;
  }

  /**
   * Adds a particle.
   * 
   * @param p The particle.
   */
  public void add(final Particle p) {
    animator.getAnimationList().addAnimated(p);
    particles.add(p);
  }

  /**
   * Removes a particle.
   * 
   * @param p The particle.
   */
  public void remove(final Particle p) {
    particles.remove(p);
  }

  /** Signals that a redraw is necessary. */
  protected void step() {
    ready.incrementAndGet();
  }

  /**
   * Setter.
   * 
   * @param running Sets the running state.
   */
  public void setRunning(final boolean running) {
    this.running = running;
  }

  /**
   * Getter.
   * 
   * @return Whether the render pass is running.
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Getter.
   * 
   * @return Whether the last redraw has been finished.
   */
  public boolean hasFinishedRedraw() {
    return finishedRedraw;
  }

  /** Clears the finished redraw flag. */
  public void ackFinishedRedraw() {
    finishedRedraw = false;
  }

  /**
   * Fades the image.
   * 
   * @param g The image.
   * @param alpha The strength of the fade.
   */
  private void fade(final Graphics2D g, final double alpha) {
    g.setColor(Color.BLACK);
    PaintUtil.setAlpha(g, 1.0 - alpha);
    g.fillRect(0, 0, getWidth(), getHeight());
  }

  /**
   * Getter.
   * 
   * @return Creates the graphics context of the image.
   */
  protected Graphics2D getGraphics() {
    return (Graphics2D) img.getGraphics();
  }

  /** Red particles. */
  private static final Color RED = new Color(1f, 0.1f, 0.1f);
  /** Green particles. */
  private static final Color GREEN = new Color(0.1f, 1f, 0.1f);
  /** Blue particles. */
  private static final Color BLUE = new Color(0.1f, 0.1f, 1f);

  /** Computes the next actual image. */
  protected void stepImage() {
    final Graphics2D g = getGraphics();
    fade(g, fade);
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, particleStrength));
    try (Snapshot<Particle> s = particles.getSnapshot()) {
      for(final Particle p : s) {
        if(p == null || !p.shouldDraw()) {
          continue;
        }
        switch(p.getColor()) {
          case Particle.RED:
            g.setColor(RED);
            break;
          case Particle.GREEN:
            g.setColor(GREEN);
            break;
          case Particle.BLUE:
            g.setColor(BLUE);
            break;
          default:
            throw new IllegalArgumentException("unknown color: " + p.getColor());
        }
        g.fill(PaintUtil.createCircle(p.getX(), p.getY(), p.getSize()));
      }
    }
    g.dispose();
    if(!Main.VIDEO_MODE) {
      finishedRedraw = true;
      return;
    }
    if(Main.makeScreenshot) return;
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        try {
          Main.makeScreenshot();
        } finally {
          finishedRedraw = true;
        }
      }

    });
  }

  @Override
  public void draw(final Graphics2D g, final KanvasContext ctx) {
    final int r = ready.getAndSet(0);
    if(r > 0) {
      if(r > 1) {
        System.err.println(getClass().getName() + " skipped " + (r - 1) + " frames");
      }
      stepImage();
    }
    g.drawImage(img, 0, 0, null);
  }

  @Override
  public void getBoundingBox(final RectangularShape bbox) {
    bbox.setFrame(0, 0, getWidth(), getHeight());
  }

  /**
   * Getter.
   * 
   * @return The width of the image.
   */
  public int getWidth() {
    return img.getWidth();
  }

  /**
   * Getter.
   * 
   * @return The height of the image.
   */
  public int getHeight() {
    return img.getHeight();
  }

}
