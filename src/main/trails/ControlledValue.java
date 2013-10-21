package trails;

import java.util.Objects;

import jkanvas.Refreshable;

/**
 * A controlled value.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public abstract class ControlledValue {

  /** The object that gets notified when a number changes. */
  private final Refreshable refreshee;
  /** The minimum value. */
  private final double minValue;
  /** The maximum value. */
  private final double maxValue;
  /** The name of the value. */
  private final String name;

  /**
   * Creates a controlled value.
   * 
   * @param name The name of the value.
   * @param refreshee The object that gets notified when the value changes.
   * @param minValue The minimum value.
   * @param maxValue The maximum value.
   */
  public ControlledValue(final String name, final Refreshable refreshee,
      final double minValue, final double maxValue) {
    this.name = Objects.requireNonNull(name);
    this.refreshee = Objects.requireNonNull(refreshee);
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  /**
   * Getter.
   * 
   * @return The name of the value.
   */
  public String getName() {
    return name;
  }

  /**
   * Setter.
   * 
   * @param value Sets the value.
   */
  public void setValue(final double value) {
    if(value < minValue) {
      setValue(minValue);
    } else if(value > maxValue) {
      setValue(maxValue);
    } else if(value == getValue()) return;
    setValueDirectly(value);
    refreshee.refresh();
  }

  /**
   * Setter.
   * 
   * @param value Sets the value. This method is only called when the value
   *          actually changes.
   */
  protected abstract void setValueDirectly(double value);

  /**
   * Getter.
   * 
   * @return The current value.
   */
  public abstract double getValue();

  /**
   * Getter.
   * 
   * @return The minimum value.
   */
  public double getMinValue() {
    return minValue;
  }

  /**
   * Getter.
   * 
   * @return The maximum value.
   */
  public double getMaxValue() {
    return maxValue;
  }

}
