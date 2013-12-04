package trails.routes;

import java.util.Objects;

import javax.swing.JTextField;

import trails.particels.ParticleProvider;

/**
 * Slices time into parts.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public abstract class TimeSlicer {

  /** The size of the time slice. */
  private long timeSlice = 1L * 24L * 60L * 60L * 1000L; // 1d
  /** The threshold. */
  private int threshold;

  /**
   * Is called every time a new slice is started.
   * 
   * @param provider The particle provider.
   * @param width The width of the image.
   * @param height The height of the image.
   */
  public abstract void timeSlice(ParticleProvider provider, int width, int height);

  /**
   * Setter.
   * 
   * @param threshold The threshold to remove aggregated trips.
   */
  public void setThreshold(final int threshold) {
    this.threshold = threshold;
  }

  /**
   * Getter.
   * 
   * @return The threshold to remove aggregated trips.
   */
  public int getThreshold() {
    return threshold;
  }

  /**
   * Setter. The value has to be checked afterwards.
   * 
   * @param timeSlice The size of a slice.
   */
  public void setTimeSlice(final long timeSlice) {
    if(timeSlice < 1000L) {
      setTimeSlice(1000L);
      return;
    }
    // adjust intervals
    final double pFrom = (double) intervalFrom / (double) this.timeSlice;
    final double pTo = (double) intervalTo / (double) this.timeSlice;
    this.timeSlice = timeSlice;
    setInterval((long) Math.floor(pFrom * timeSlice),
        (long) Math.ceil(pTo * timeSlice));
  }

  /**
   * Getter.
   * 
   * @return The size of a slice.
   */
  public long getTimeSlice() {
    return timeSlice;
  }

  /** The start of the interval. */
  private long intervalFrom = 12L * 60L * 60L * 1000L; // 12h
  /** The end of the interval exclusive. */
  private long intervalTo = 15L * 60L * 60L * 1000L; // 15h

  /**
   * Setter. The value has to be checked afterwards.
   * 
   * @param from The start of the interval inclusive.
   * @param to The end of the interval exclusive.
   */
  public void setInterval(final long from, final long to) {
    if(from < 0) {
      setInterval(0, to);
      return;
    }
    if(to > timeSlice) {
      setInterval(from, timeSlice);
      return;
    }
    if(from >= to) {
      if(to > 0) {
        setInterval(to - 1, to);
      } else {
        setInterval(to, to + 1);
      }
      return;
    }
    intervalFrom = from;
    intervalTo = to;
  }

  /**
   * Setter.
   * 
   * @param from The percent of the start inclusive.
   * @param to The percent of the end exclusive.
   */
  public void setPercent(final double from, final double to) {
    setInterval((long) Math.floor(from * timeSlice), (long) Math.ceil(to * timeSlice));
  }

  /**
   * Getter.
   * 
   * @return The interval start inclusive.
   */
  public long getIntervalFrom() {
    return intervalFrom;
  }

  /**
   * Getter.
   * 
   * @return The percent of the start inclusive.
   */
  public double getPercentFrom() {
    return (double) intervalFrom / timeSlice;
  }

  /**
   * Getter.
   * 
   * @return The interval end exclusive.
   */
  public long getIntervalTo() {
    return intervalTo;
  }

  /**
   * Getter.
   * 
   * @return The percent of the end exclusive.
   */
  public double getPercentTo() {
    return (double) intervalTo / timeSlice;
  }

  /**
   * Getter.
   * 
   * @param from The start.
   * @param to The end.
   * @return How many slices this span includes.
   */
  protected int getNumberOfSlices(final long from, final long to) {
    return (int) ((to - from) / (intervalTo - intervalFrom)) + 1;
  }

  /** The text info field. */
  private JTextField info;

  /**
   * Setter.
   * 
   * @param info The text info field.
   */
  public void setInfo(final JTextField info) {
    this.info = info;
  }

  /**
   * Setter.
   * 
   * @param text Sets the text of the text info field if any.
   */
  public void setInfoText(final String text) {
    Objects.requireNonNull(text);
    if(info == null) return;
    info.setText(text);
  }

}
