package trails;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

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
  private final double[] slotsA;
  /** The second slots. */
  private final double[] slotsB;
  /** The sum of both slots. */
  private final double[] sum;
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
    slotsA = new double[slots];
    slotsB = new double[slots];
    sum = new double[slots];
    off = 0;
  }

  /**
   * Getter.
   * 
   * @return The number of slots.
   */
  public int size() {
    return slotsA.length;
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
    if(t > 0) return t % slotsA.length;
    return (slotsA.length - (-t % slotsA.length)) % slotsA.length;
  }

  /**
   * Getter.
   * 
   * @param i The index.
   * @param slotA Whether slotA should is returned.
   * @return The value at the given index.
   */
  public double get(final int i, final boolean slotA) {
    return slotA ? slotsA[index(i)] : slotsB[index(i)];
  }

  /**
   * Setter.
   * 
   * @param i The index.
   * @param slotA Whether slotA should be written.
   * @param v The value at the given index.
   */
  public void set(final int i, final boolean slotA, final double v) {
    if(v < 0) throw new IllegalArgumentException("" + v);
    final int index = index(i);
    if(slotA) {
      slotsA[index] = v;
    } else {
      slotsB[index] = v;
    }
    sum[index] = slotsA[index] + slotsB[index];
  }

  @Override
  public void draw(final Graphics2D g, final KanvasContext ctx) {
    final double slotHeight = ArrayUtil.max(sum);
    if(slotHeight == 0) return;
    final int sCount = slotsA.length;
    final double slotWidth = sCount;
    final Rectangle2D rect = new Rectangle2D.Double();
    double x = 0.0;
    for(int b = 0; b < sCount; ++b) {
      final double w = width / slotWidth;
      final double hA = get(b, true) * height / slotHeight;
      final double hB = get(b, false) * height / slotHeight;
      final double h = hA + hB;
      rect.setFrame(x, height - h, w, hA);
      g.setColor(b == 0 ? new Color(0x990000) : new Color(0xfc8d59));
      g.fill(rect);
      g.setColor(Color.BLACK);
      g.draw(rect);
      rect.setFrame(x, height - hB, w, hB);
      g.setColor(b == 0 ? new Color(0x034e7b) : new Color(0x2b8cbe));
      g.fill(rect);
      g.setColor(Color.BLACK);
      g.draw(rect);
      x += w;
    }
  }

  @Override
  public void getBoundingBox(final RectangularShape bbox) {
    bbox.setFrame(0, 0, width, height);
  }

}
