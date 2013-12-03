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

  void setThreshold(int t);

  int getThreshold();

  void setTimeSlice(long slice);

  long getTimeSlice();

}
