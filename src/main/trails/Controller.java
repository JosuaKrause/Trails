package trails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jkanvas.Refreshable;

/**
 * The controller controls the values that can be customized.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Controller {

  /** The list of controlled values. */
  private final List<ControlledValue> values;

  /**
   * Creates a new controller.
   * 
   * @param refreshee The object that will be notified when a value changes.
   */
  public Controller(final Refreshable refreshee) {
    values = new ArrayList<>();
    values.add(new ControlledValue("Curve Bend", refreshee, 0, 1) {

      @Override
      protected void setValueDirectly(final double value) {
        Particle.bendRatio = value;
      }

      @Override
      public double getValue() {
        return Particle.bendRatio;
      }

    });
    values.add(new ControlledValue("Particle Brightness", refreshee, 0.01, 0.3) {

      @Override
      protected void setValueDirectly(final double value) {
        TrailRenderpass.particleStrength = 1f - (float) value;
      }

      @Override
      public double getValue() {
        return 1.0 - TrailRenderpass.particleStrength;
      }

    });
    values.add(new ControlledValue("Trail Length", refreshee, 0.7, 0.99) {

      @Override
      protected void setValueDirectly(final double value) {
        TrailRenderpass.fade = value;
      }

      @Override
      public double getValue() {
        return TrailRenderpass.fade;
      }

    });
    values.add(new ControlledValue("Aggregation Radius", refreshee, 0.0, 100.0) {

      @Override
      protected void setValueDirectly(final double value) {
        // TODO Auto-generated method stub
      }

      @Override
      public double getValue() {
        // TODO Auto-generated method stub
        return 0;
      }

    });
  }

  /**
   * Getter.
   * 
   * @return The controlled values.
   */
  public Iterable<ControlledValue> values() {
    return Collections.unmodifiableCollection(values);
  }

}
