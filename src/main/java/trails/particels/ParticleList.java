package trails.particels;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jkanvas.util.SnapshotList;
import jkanvas.util.SnapshotList.Snapshot;

/**
 * A list that is optimized for adding and removing many objects.
 * 
 * @author Joschi <josua.krause@gmail.com>
 * @param <T> The object type.
 */
public class ParticleList<T extends Revokable> {
  /** The snapshot list. */
  private final SnapshotList<T> list;
  /** The set of active elements. */
  private final Map<T, T> set;

  /** Creates an empty particle list. */
  public ParticleList() {
    list = new SnapshotList<T>() {

      @Override
      protected boolean isValid(final T el) {
        return el != null && el.isActive();
      }

    };
    set = new ConcurrentHashMap<>();
  }

  /**
   * Adds an element to the list.
   * 
   * @param el The element to add.
   */
  public void add(final T el) {
    Objects.requireNonNull(el);
    list.add(el);
    set.put(el, el);
  }

  /**
   * Removes an element from the list.
   * 
   * @param el The element.
   */
  public void remove(final T el) {
    Objects.requireNonNull(el);
    el.deactivate();
    set.remove(el);
  }

  /**
   * Getter.
   * 
   * @return Gets a snapshot of the list.
   */
  public Snapshot<T> getSnapshot() {
    return list.getSnapshot();
  }

}
