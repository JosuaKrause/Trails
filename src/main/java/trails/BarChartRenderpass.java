package trails;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import jkanvas.KanvasContext;
import jkanvas.painter.Renderpass;
import jkanvas.util.ArrayUtil;

/**
 * Renders a bar chart with the first bar highlighted.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class BarChartRenderpass extends Renderpass {

  /** The slots. */
  private final double[] slots;
  /** The offset. */
  private int off;
  /** The width of the chart. */
  private final double width;
  /** The height of the chart. */
  private final double height;

  /**
   * Creates a new bar chart render pass.
   * 
   * @param slots The number of slots.
   * @param width The total width.
   * @param height The total height.
   */
  public BarChartRenderpass(final int slots, final double width, final double height) {
    this.width = width;
    this.height = height;
    this.slots = new double[slots];
    off = 0;
  }

  /**
   * Getter.
   * 
   * @return The number of slots.
   */
  public int size() {
    return slots.length;
  }

  /**
   * Rotates all slots.
   * 
   * @param by The number of slots to rotate. Positive shift means rotating to
   *          the left.
   */
  public void shift(final int by) {
    off = index(by);
  }

  /**
   * Computes the actual index for the given index.
   * 
   * @param i The given index.
   * @return The actual index in the array.
   */
  private int index(final int i) {
    final int t = off + i;
    if(t > 0) return t % slots.length;
    return (slots.length - (-t % slots.length)) % slots.length;
  }

  /**
   * Getter.
   * 
   * @param i The index.
   * @return The value at the given index.
   */
  public double get(final int i) {
    return slots[index(i)];
  }

  /**
   * Setter.
   * 
   * @param i The index.
   * @param v The value at the given index.
   */
  public void set(final int i, final double v) {
    if(v < 0) throw new IllegalArgumentException("" + v);
    slots[index(i)] = v;
  }

  @Override
  public void draw(final Graphics2D g, final KanvasContext ctx) {
    final double slotHeight = ArrayUtil.max(slots);
    if(slotHeight == 0) return;
    final int sCount = slots.length;
    final double slotWidth = sCount;
    final Rectangle2D rect = new Rectangle2D.Double();
    double x = 0.0;
    for(int b = 0; b < sCount; ++b) {
      final double w = width / slotWidth;
      final double h = get(b) * height / slotHeight;
      rect.setFrame(x, height - h, w, h);
      g.setColor(b == 0 ? new Color(0x034e7b) : new Color(0x2b8cbe));
      g.fill(rect);
      g.setColor(Color.BLACK);
      g.draw(rect);
      x += w;
    }
  }

  @Override
  public void getBoundingBox(final Rectangle2D bbox) {
    bbox.setFrame(0, 0, width, height);
  }

}
