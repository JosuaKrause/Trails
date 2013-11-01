package trails;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;

import jkanvas.Canvas;
import jkanvas.animation.AnimatedPainter;
import jkanvas.examples.ExampleUtil;
import jkanvas.util.Resource;
import trails.controls.ControlPanel;
import trails.controls.ControlledValue;
import trails.controls.Controller;
import trails.io.TripManager;
import trails.particels.ParticleProvider;
import trails.particels.TimeSlicer;
import trails.particels.TrailRenderpass;
import trails.routes.TripSlicer;

/**
 * Starts the main project.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Main {

  /**
   * Starts the main application.
   * 
   * @param args No arguments.
   * @throws IOException I/O Exception.
   */
  public static void main(final String[] args) throws IOException {
    final AnimatedPainter p = new AnimatedPainter() {

      private long time = 0;

      @Override
      protected long getTime() {
        if(isStopped()) return time;
        final long t = time;
        time += 2;
        return t;
      }

    };
    p.setFramerate(60);
    final Canvas c = new Canvas(p, true, 1024, 768);
    final JFrame frame = new JFrame("Trails") {

      @Override
      public void dispose() {
        c.dispose();
        super.dispose();
      }

    };
    final TrailRenderpass trails = new TrailRenderpass(p, 500, 500);
    final Resource origin = Resource.getFor("trip_data_1.csv.zip");
    final Resource bin = new Resource(
        (String) null, "trip_data_1.dat", (String) null, (String) null);
    final TimeSlicer slicer = new TripSlicer(TripManager.getManager(bin, origin));
    final ParticleProvider provider = new ParticleProvider(p, trails, slicer, 1000);

    final Controller ctrl = new Controller(c);
    ctrl.addControlledValue(new ControlledValue("Speed", c, 100.0, 2000.0) {

      @Override
      protected void setValueDirectly(final double value) {
        provider.setSliceTime((long) value);
      }

      @Override
      public double getValue() {
        return provider.getSliceTime();
      }

    });

    ExampleUtil.setupCanvas(frame, c, p, true, true, true, true);
    frame.setLayout(new BorderLayout());
    frame.remove(c);
    frame.add(c, BorderLayout.CENTER);
    frame.add(new ControlPanel(ctrl), BorderLayout.WEST);
    frame.pack();
    p.addPass(trails);
    c.reset();
  }
}
