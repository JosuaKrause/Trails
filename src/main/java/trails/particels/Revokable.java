package trails.particels;

/**
 * This interface marks a object to be able to be switched off.
 * 
 * @author Joschi <josua.krause@gmail.com>
 */
public interface Revokable {

  /**
   * Switches this object off.
   * 
   * @see #isActive()
   */
  void deactivate();

  /**
   * Getter.
   * 
   * @return Whether this object is still active.
   * @see Revokable#deactivate()
   */
  boolean isActive();

}
