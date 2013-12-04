package trails.controls;

import javax.swing.JComponent;

/**
 * A class to enable refreshing of values for components.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public interface ValueRefresher {

  /** Refreshes the values. */
  void refreshValue();

  /**
   * Getter.
   * 
   * @return The description of the time panel.
   */
  String getDescription();

  /**
   * Getter.
   * 
   * @return The component.
   */
  JComponent getComponent();

}
