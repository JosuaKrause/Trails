package trails.routes;

import trails.particels.ParticleProvider;

/**
 * Slices time into parts.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public interface TimeSlicer {

  /**
   * Is called every time a new slice is started.
   * 
   * @param provider The particle provider.
   * @param width The width of the image.
   * @param height The height of the image.
   */
  void timeSlice(ParticleProvider provider, int width, int height);

  /**
   * Setter.
   * 
   * @param t The threshold to remove aggregated trips.
   */
  void setThreshold(int t);

  /**
   * Getter.
   * 
   * @return The threshold to remove aggregated trips.
   */
  int getThreshold();

  /**
   * Setter.
   * 
   * @param slice The size of a slice.
   */
  void setTimeSlice(long slice);

  /**
   * Getter.
   * 
   * @return The size of a slice.
   */
  long getTimeSlice();

}
