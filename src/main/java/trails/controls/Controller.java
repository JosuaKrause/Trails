package trails.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;

import jkanvas.Refreshable;
import trails.particels.Particle;
import trails.particels.TrailRenderpass;

/**
 * The controller controls the values that can be customized.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Controller {

  /** The list of controlled values. */
  private final List<ControlledValue> values;
  /** The list of time panels. */
  private final List<ValueRefresher> vrs;

  /**
   * Creates a new controller.
   * 
   * @param refreshee The object that will be notified when a value changes.
   */
  public Controller(final Refreshable refreshee) {
    vrs = new ArrayList<>();
    values = new ArrayList<>();
    values.add(new ControlledValue("Curve Bend", refreshee, 0, 1, 100) {

      @Override
      protected void setValueDirectly(final double value) {
        Particle.bendRatio = value;
      }

      @Override
      public double getValue() {
        return Particle.bendRatio;
      }

    });
    values.add(new ControlledValue("Particle Brightness", refreshee, 0.01, 0.3, 100) {

      @Override
      protected void setValueDirectly(final double value) {
        TrailRenderpass.particleStrength = 1f - (float) value;
      }

      @Override
      public double getValue() {
        return 1.0 - TrailRenderpass.particleStrength;
      }

    });
    values.add(new ControlledValue("Trail Length", refreshee, 0.7, 0.99, 100) {

      @Override
      protected void setValueDirectly(final double value) {
        TrailRenderpass.fade = value;
      }

      @Override
      public double getValue() {
        return TrailRenderpass.fade;
      }

    });
  }

  /**
   * Adds another controlled value.
   * 
   * @param val The controlled value.
   */
  public void addControlledValue(final ControlledValue val) {
    Objects.requireNonNull(val);
    values.add(val);
  }

  /**
   * Adds another value refresher.
   * 
   * @param pan The value refresher.
   */
  public void addValueRefresher(final ValueRefresher pan) {
    Objects.requireNonNull(pan);
    vrs.add(pan);
  }

  /**
   * Getter.
   * 
   * @return The controlled values.
   */
  public Iterable<ControlledValue> values() {
    return Collections.unmodifiableCollection(values);
  }

  /**
   * Getter.
   * 
   * @return The value refreshers.
   */
  public Iterable<ValueRefresher> refresher() {
    return Collections.unmodifiableCollection(vrs);
  }

  /**
   * Refreshes all times.
   * 
   * @param except The panel to skip.
   */
  public void refreshTimes(final ValueRefresher except) {
    for(final ValueRefresher pan : vrs) {
      if(pan == except) {
        continue;
      }
      pan.refreshValue();
    }
  }

  /**
   * Adds a component.
   * 
   * @param name The name of the component.
   * @param comp The component.
   */
  public void addComponent(final String name, final JComponent comp) {
    Objects.requireNonNull(comp);
    addValueRefresher(new ValueRefresher() {

      @Override
      public void refreshValue() {
        // nothing to do
      }

      @Override
      public String getDescription() {
        return name;
      }

      @Override
      public JComponent getComponent() {
        return comp;
      }

    });
  }

}
