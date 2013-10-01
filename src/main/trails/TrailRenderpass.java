package trails;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jkanvas.KanvasContext;
import jkanvas.animation.Animated;
import jkanvas.animation.Animator;
import jkanvas.painter.RenderpassAdapter;
import jkanvas.util.PaintUtil;

public class TrailRenderpass extends RenderpassAdapter {

  private final BufferedImage img;

  private final Animator animator;

  private final Animated animated;

  private final ParticleList<Particle> particles;

  protected AtomicInteger ready;

  protected volatile boolean running;

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

  public void add(final Particle p) {
    animator.getAnimationList().addAnimated(p);
    particles.add(p);
  }

  public void remove(final Particle p) {
    particles.remove(p);
  }

  protected void step() {
    ready.incrementAndGet();
  }

  public void setRunning(final boolean running) {
    this.running = running;
  }

  public boolean isRunning() {
    return running;
  }

  private void fade(final Graphics2D g, final double alpha) {
    g.setColor(Color.BLACK);
    PaintUtil.setAlpha(g, 1.0 - alpha);
    g.fillRect(0, 0, getWidth(), getHeight());
  }

  protected Graphics2D getGraphics() {
    return (Graphics2D) img.getGraphics();
  }

  protected void stepImage() {
    final Graphics2D g = getGraphics();
    fade(g, 0.95);
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 0.97f));
    g.setColor(new Color(0.1f, 0.1f, 1f));
    for(final Particle p : particles) {
      g.fill(PaintUtil.createCircle(p.getX(), p.getY(), p.getSize()));
    }
    g.dispose();
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
  public Rectangle2D getBoundingBox() {
    return new Rectangle2D.Double(0, 0, getWidth(), getHeight());
  }

  public int getWidth() {
    return img.getWidth();
  }

  public int getHeight() {
    return img.getHeight();
  }

}
