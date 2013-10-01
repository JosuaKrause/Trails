package trails;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import jkanvas.KanvasContext;
import jkanvas.animation.Animated;
import jkanvas.animation.Animator;
import jkanvas.painter.RenderpassAdapter;
import jkanvas.util.PaintUtil;

public class TrailRenderpass extends RenderpassAdapter {

  private final BufferedImage img;

  private final Animated animated;

  protected AtomicInteger ready;

  protected volatile boolean running;

  public TrailRenderpass(final Animator animator, final int width, final int height) {
    img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    ready = new AtomicInteger();
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
    fade(g, 0.8);
    final Random r = ThreadLocalRandom.current();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 0.9f));
    g.setColor(new Color(0.1f, 0.1f, 1f));
    final double w = getWidth();
    final double h = getHeight();
    for(int i = 0; i < 1000; ++i) {
      final Point2D pos = new Point2D.Double(
          (w + r.nextGaussian() * w * 0.125) * 0.5,
          (h + r.nextGaussian() * h * 0.125) * 0.5);
      g.fill(PaintUtil.createCircle(pos.getX(), pos.getY(), 5));
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
