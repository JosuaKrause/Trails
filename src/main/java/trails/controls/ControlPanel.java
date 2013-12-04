package trails.controls;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The control panel.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public final class ControlPanel extends JPanel {

  /**
   * Creates a control panel.
   * 
   * @param ctrl The corresponding controller.
   */
  public ControlPanel(final Controller ctrl) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    final Component space = Box.createRigidArea(new Dimension(5, 5));

    for(final ControlledValue v : ctrl.values()) {
      final double step = (v.getMaxValue() - v.getMinValue()) / 100.0;
      final SpinnerNumberModel m = new SpinnerNumberModel(
          v.getValue(), v.getMinValue(), v.getMaxValue(), step);
      final JSpinner spinner = new JSpinner(m);
      spinner.setMaximumSize(new Dimension(100, 40));
      spinner.setPreferredSize(new Dimension(100, 40));
      spinner.addChangeListener(new ChangeListener() {

        @Override
        public void stateChanged(final ChangeEvent e) {
          v.setValue((double) spinner.getValue());
        }

      });
      addHor(this, new JLabel(v.getName() + ":"), Box.createHorizontalGlue(), spinner);
      add(space);
    }

    for(final ValueRefresher pan : ctrl.refresher()) {
      if(pan.getDescription() != null) {
        addHor(this, new JLabel(pan.getDescription() + ":"),
            Box.createHorizontalGlue(), pan.getComponent());
      } else {
        addHor(this, Box.createHorizontalGlue(), pan.getComponent());
      }
      add(space);
    }

    // end of layout
    add(Box.createVerticalGlue());
  }

  /**
   * Adds a number of components to the given panel.
   * 
   * @param panel The panel to add the components to.
   * @param comps The components to add.
   */
  private static void addHor(final JPanel panel, final Component... comps) {
    final JPanel hor = new JPanel();
    hor.setLayout(new BoxLayout(hor, BoxLayout.X_AXIS));
    for(final Component c : comps) {
      hor.add(Box.createRigidArea(new Dimension(5, 5)));
      if(c != null) {
        hor.add(c);
      }
    }
    hor.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(hor);
  }

}
