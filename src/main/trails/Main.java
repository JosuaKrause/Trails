package trails;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import jkanvas.Canvas;
import jkanvas.FrameRateDisplayer;
import jkanvas.animation.AnimatedPainter;
import jkanvas.examples.ExampleUtil;
import jkanvas.painter.SimpleTextHUD;
import jkanvas.util.Resource;
import jkanvas.util.Screenshot;
import trails.controls.ControlPanel;
import trails.controls.ControlledValue;
import trails.controls.Controller;
import trails.io.BinaryTripManager;
import trails.io.SQLHandler;
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

  /** Whether to use the SQL trips. */
  public static final boolean SQL_TRIPS = true;
  /** The video mode. */
  public static final boolean VIDEO_MODE = false;
  /** The trail render pass. */
  protected static TrailRenderpass trails;
  /** The current time. */
  protected static long time = 0;
  /** The frame. */
  private static JFrame frame;
  /** Whether a screenshot is currently made. */
  public static volatile boolean makeScreenshot;
  /** The canvas. */
  protected static Canvas c;
  /** The help. */
  private static SimpleTextHUD help;

  /** Makes a screenshot. */
  public static void makeScreenshot() {
    if(makeScreenshot) return;
    final boolean h = help.isVisible();
    final FrameRateDisplayer frd = c.getFrameRateDisplayer();
    c.setFrameRateDisplayer(null);
    help.setVisible(false);
    try {
      Screenshot.SCALE = 1;
      makeScreenshot = true;
      System.out.println("started screenshot");
      final File saved = Screenshot.savePNG(new File("pics/"),
          "frame" + time, frame.getRootPane());
      System.out.println("saved " + saved);
    } catch(final IOException e) {
      e.printStackTrace();
    } finally {
      makeScreenshot = false;
      help.setVisible(h);
      c.setFrameRateDisplayer(frd);
    }
  }

  /**
   * Starts the main application.
   * 
   * @param args No arguments.
   * @throws Exception Exception.
   */
  public static void main(final String[] args) throws Exception {
    final AnimatedPainter p = new AnimatedPainter() {

      @Override
      protected long getTime() {
        if(isStopped()) return time;
        if(trails == null || !trails.hasFinishedRedraw()) return time;
        trails.ackFinishedRedraw();
        final long t = time;
        time += 5;
        return t;
      }

    };
    p.setFramerate(60);
    c = new Canvas(p, true, 1024, 768);
    trails = new TrailRenderpass(p, 500, 500);
    final TripManager mng;
    if(SQL_TRIPS) {
      mng = new SQLHandler("gps_trips.db");
    } else {
      mng = new BinaryTripManager(Resource.getFor("trip_data_1.dat"));
    }
    frame = new JFrame("Trails") {

      @Override
      public void dispose() {
        c.dispose();
        try {
          mng.close();
        } catch(final Exception e) {
          e.printStackTrace();
        }
        super.dispose();
      }

    };
    final TimeSlicer slicer = new TripSlicer(mng);
    System.out.println("time slicer initialized");
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
    help = ExampleUtil.setupCanvas(frame, c, p, true, true, true, true);
    frame.setLayout(new BorderLayout());
    frame.remove(c);
    frame.add(c, BorderLayout.CENTER);
    frame.add(new ControlPanel(ctrl), BorderLayout.WEST);
    frame.pack();
    p.addPass(trails);
    c.reset();
  }
}
