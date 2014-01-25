package trails;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jkanvas.Canvas;
import jkanvas.FrameRateDisplayer;
import jkanvas.animation.AnimatedPainter;
import jkanvas.animation.AnimationTiming;
import jkanvas.examples.ExampleUtil;
import jkanvas.painter.Renderpass;
import jkanvas.painter.SimpleTextHUD;
import jkanvas.painter.groups.LinearGroup;
import jkanvas.painter.groups.RenderGroup;
import jkanvas.painter.pod.BorderRenderpass;
import jkanvas.util.Resource;
import jkanvas.util.Screenshot;
import trails.controls.ControlPanel;
import trails.controls.ControlledValue;
import trails.controls.Controller;
import trails.controls.RangeSlider;
import trails.controls.TimePanel;
import trails.io.BinaryTripManager;
import trails.io.SQLHandler;
import trails.io.TripManager;
import trails.particels.ParticleProvider;
import trails.particels.TrailRenderpass;
import trails.routes.TimeSlicer;
import trails.routes.TripSlicer;

/**
 * Starts the main project.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public class Main {

  /** The binary source. */
  public static final int BIN_SOURCE = 0;
  /** The GPS source. */
  public static final int GPS_SOURCE = 1;
  /** The DC source. */
  public static final int DC_SOURCE = 2;
  /** Whether to use the SQL trips. */
  public static final int TRIPS_SOURCE = DC_SOURCE;
  /** The video mode. */
  public static final boolean VIDEO_MODE = false;
  /** The start time offset. */
  public static final long INIT_TIME = (9L * 31L + 1L) * 24L * 60L * 60L * 1000L;
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
  /** Frame counter. */
  private static long numberOfFrames = 0;

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
      final File saved = Screenshot.save(new File("pics/"), "frame",
          Screenshot.padNumber(numberOfFrames, 8) + ".png", frame.getRootPane());
      ++numberOfFrames;
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
    c = new Canvas(p, true, 600, 600);
    trails = new TrailRenderpass(p, 500, 500);
    final TripManager mng;
    switch(TRIPS_SOURCE) {
      case GPS_SOURCE:
        mng = new SQLHandler("gps_trips");
        break;
      case DC_SOURCE:
        mng = new SQLHandler("dc_trips");
        break;
      case BIN_SOURCE:
        mng = new BinaryTripManager(Resource.getFor("trip_data_1.dat"));
        break;
      default:
        throw new AssertionError("invalid source: " + TRIPS_SOURCE);
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
    final RenderGroup<Renderpass> main = new LinearGroup<>(p, false, 5,
        AnimationTiming.NO_ANIMATION);
    main.addRenderpass(trails);
    final BarChartRenderpass bc = new BarChartRenderpass(60, 450, 30);
    final TimeSlicer slicer = new TripSlicer(mng, bc, INIT_TIME);
    main.addRenderpass(new BorderRenderpass<>(bc));
    System.out.println("time slicer initialized");
    final ParticleProvider provider = new ParticleProvider(p, trails, slicer, 500);
    final Controller ctrl = initCtrl(provider, slicer);
    help = ExampleUtil.setupCanvas(frame, c, p, true, true, true, true);
    frame.setLayout(new BorderLayout());
    frame.remove(c);
    frame.add(c, BorderLayout.CENTER);
    frame.add(new ControlPanel(ctrl), BorderLayout.WEST);
    frame.pack();
    frame.setLocationRelativeTo(null);
    p.addPass(main);
    c.reset();
  }

  /**
   * Initializes the controller.
   * 
   * @param provider The particle provider.
   * @param slicer The time slicer.
   * @return The controller.
   */
  public static Controller initCtrl(
      final ParticleProvider provider, final TimeSlicer slicer) {
    final Controller ctrl = new Controller(c);
    ctrl.addControlledValue(new ControlledValue("Slice Time", c, 100.0, 2000.0, 100) {

      @Override
      protected void setValueDirectly(final double value) {
        provider.setSliceTime((long) value);
      }

      @Override
      public double getValue() {
        return provider.getSliceTime();
      }

    });
    ctrl.addControlledValue(new ControlledValue("Threshold", c, 0, 1000, 1000) {

      @Override
      protected void setValueDirectly(final double value) {
        slicer.setThreshold((int) value);
      }

      @Override
      public double getValue() {
        return slicer.getThreshold();
      }

    });
    ctrl.addValueRefresher(new TimePanel("Slice") {

      @Override
      protected long parentTime() {
        return slicer.getTimeSlice();
      }

      @Override
      protected void onChange(final long time) {
        slicer.setTimeSlice(time);
        ctrl.refreshTimes(this);
      }

    });
    ctrl.addValueRefresher(new TimePanel("From") {

      @Override
      protected long parentTime() {
        return slicer.getIntervalFrom();
      }

      @Override
      protected void onChange(final long time) {
        slicer.setInterval(time, slicer.getIntervalTo());
        ctrl.refreshTimes(this);
      }

    });
    ctrl.addValueRefresher(new TimePanel("To") {

      @Override
      protected long parentTime() {
        return slicer.getIntervalTo();
      }

      @Override
      protected void onChange(final long time) {
        slicer.setInterval(slicer.getIntervalFrom(), time);
        ctrl.refreshTimes(this);
      }

    });
    ctrl.addValueRefresher(new RangeSlider(0, 100) {

      private int change;

      @Override
      public void refreshValue() {
        ++change;
        final double pFrom = slicer.getPercentFrom();
        final double pTo = slicer.getPercentTo();
        setValue((int) Math.floor(pFrom * 100.0));
        setUpperValue((int) Math.ceil(pTo * 100.0));
        --change;
      }

      @Override
      protected void onChange() {
        if(change > 0) return;
        slicer.setPercent(getValue() / 100.0, getUpperValue() / 100.0);
        ctrl.refreshTimes(null);
      }

      @Override
      public String getDescription() {
        return "Interval";
      }

    });
    final JCheckBox cb = new JCheckBox("Skip empty slices", TripSlicer.isSkippingGaps());
    cb.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(final ChangeEvent e) {
        TripSlicer.setSkipGaps(cb.isSelected());
      }

    });
    ctrl.addComponent(null, cb);
    final JTextField infoA = new JTextField();
    infoA.setMaximumSize(new Dimension(400, 40));
    infoA.setPreferredSize(new Dimension(320, 40));
    final JTextField infoB = new JTextField();
    infoB.setMaximumSize(new Dimension(400, 40));
    infoB.setPreferredSize(new Dimension(320, 40));
    slicer.setInfo(infoA, infoB);
    ctrl.addComponent("Current Start", infoA);
    ctrl.addComponent("Current End", infoB);
    return ctrl;
  }

}
