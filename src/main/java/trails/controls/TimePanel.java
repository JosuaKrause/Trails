package trails.controls;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A time panel allows to edit time spans.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public abstract class TimePanel extends JPanel implements ValueRefresher {

  /** The spinner. */
  private final List<JSpinner> spinner = new ArrayList<>();
  /** The ranges. */
  private final List<Long> ranges = new ArrayList<>();
  /** The change listener. */
  private final ChangeListener change = new ChangeListener() {

    @Override
    public void stateChanged(final ChangeEvent e) {
      if(onChange > 0) return;
      setTime(getTime());
    }

  };
  /** Is set during change. */
  protected int onChange;
  /** The description of the time panel. */
  private final String name;

  /**
   * Creates a time panel.
   * 
   * @param name The description of the time panel.
   */
  public TimePanel(final String name) {
    this.name = name;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    addSpinner("M", 12L);
    addSpinner("d", 30L);
    addSpinner("h", 24L);
    addSpinner("m", 60L);
    addSpinner("s", 60L);
    setTimeDirectly(parentTime());
  }

  @Override
  public String getDescription() {
    return name;
  }

  @Override
  public void refreshValue() {
    setTime(parentTime());
  }

  /**
   * Getter.
   * 
   * @return The time span represented by this time panel.
   */
  public long getTime() {
    long sum = 0;
    for(int pos = 0; pos < spinner.size(); ++pos) {
      sum *= ranges.get(pos);
      sum += (Long) spinner.get(pos).getValue();
    }
    return sum * 1000L; // milli-seconds
  }

  /**
   * Setter.
   * 
   * @param millis The time span in milliseconds.
   */
  private void setTimeDirectly(final long millis) {
    ++onChange;
    long time = Math.max(millis / 1000L, 0L);
    for(int pos = spinner.size() - 1; pos >= 0; --pos) {
      final long range = ranges.get(pos);
      spinner.get(pos).setValue(time % range);
      time /= range;
    }
    --onChange;
  }

  /**
   * Setter.
   * 
   * @param millis The time span represented by this time panel.
   */
  public void setTime(final long millis) {
    if(onChange > 0) return;
    ++onChange;
    setTimeDirectly(millis);
    onChange(getTime());
    if(getTime() != parentTime()) {
      setTimeDirectly(parentTime());
    }
    --onChange;
  }

  /**
   * This method is called when the time changes.
   * 
   * @param time The new time.
   */
  protected abstract void onChange(long time);

  /**
   * Getter.
   * 
   * @return This method is called to initialize the time panel.
   */
  protected abstract long parentTime();

  /**
   * Adds a spinner.
   * 
   * @param name The name.
   * @param max The range.
   */
  private void addSpinner(final String name, final long max) {
    final JSpinner spin = new JSpinner(new SpinnerModel() {

      private long value;

      @Override
      public void setValue(final Object value) {
        final long v = (Long) value;
        if(v == this.value) return;
        this.value = v;
        for(final ChangeListener l : listeners) {
          l.stateChanged(new ChangeEvent(this));
        }
      }

      private final List<ChangeListener> listeners = new ArrayList<>();

      @Override
      public void removeChangeListener(final ChangeListener l) {
        listeners.remove(l);
      }

      @Override
      public Object getValue() {
        return value;
      }

      @Override
      public Object getPreviousValue() {
        return value - 1L;
      }

      @Override
      public Object getNextValue() {
        return value + 1L;
      }

      @Override
      public void addChangeListener(final ChangeListener l) {
        listeners.add(l);
      }

    });
    spin.addChangeListener(change);
    spin.setMaximumSize(new Dimension(60, 40));
    spin.setPreferredSize(new Dimension(60, 40));
    spinner.add(spin);
    add(spin);
    add(new JLabel(name));
    ranges.add(max);
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

}
