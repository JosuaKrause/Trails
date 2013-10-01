package trails;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import jkanvas.KanvasContext;
import jkanvas.animation.Animated;
import jkanvas.animation.Animator;
import jkanvas.painter.RenderpassAdapter;
import jkanvas.util.PaintUtil;

public class TrailRenderpass extends RenderpassAdapter {

  private final BufferedImage a;

  private final BufferedImage b;

  private final Animated animated;

  private boolean activeA;

  protected volatile boolean running;

  public TrailRenderpass(final Animator animator, final int width, final int height) {
    a = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    b = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    activeA = true;
    final Graphics2D g = (Graphics2D) a.getGraphics();
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

  public void setRunning(final boolean running) {
    this.running = running;
  }

  public boolean isRunning() {
    return running;
  }

  private void copy(final BufferedImage from, final BufferedImage to, final double alpha) {
    final Graphics2D g = (Graphics2D) to.getGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, to.getWidth(), to.getHeight());
    PaintUtil.setAlpha(g, alpha);
    g.drawImage(from, 0, 0, null);
    g.dispose();
  }

  private void flipImages(final double alpha) {
    activeA = !activeA;
    if(activeA) {
      copy(b, a, alpha);
    } else {
      copy(a, b, alpha);
    }
  }

  protected Graphics2D getGraphics() {
    return (Graphics2D) (activeA ? a.getGraphics() : b.getGraphics());
  }

  protected BufferedImage getImage() {
    return activeA ? a : b;
  }

  protected void step() {
    flipImages(0.8);
    final Random r = ThreadLocalRandom.current();
    final BufferedImage img = getImage();
    final Graphics2D g = getGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 0.9f));
    g.setColor(new Color(0.1f, 0.1f, 1f));
    final double w = img.getWidth();
    final double h = img.getHeight();
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
    g.drawImage(getImage(), 0, 0, null);
  }

  @Override
  public Rectangle2D getBoundingBox() {
    final BufferedImage img = getImage();
    return new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight());
  }

}
