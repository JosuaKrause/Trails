package trails;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ParticleList<T> implements Iterable<T> {

  private Object[] list;

  private int lowestFree = 0;

  private int length = 0;

  private int count = 0;

  private volatile int modCount = 0;

  public ParticleList() {
    this(100);
  }

  public ParticleList(final int initialSize) {
    list = new Object[initialSize];
  }

  private void ensureSize(final int size) {
    if(size < list.length) return;
    final Object[] newList = new Object[size * 2 + 1];
    System.arraycopy(list, 0, newList, 0, length);
    list = newList;
  }

  private void shrinkMaybe() {
    if(length >= list.length / 3) return;
    final Object[] newList = new Object[length * 2 + 1];
    System.arraycopy(list, 0, newList, 0, length);
    list = newList;
  }

  private void compact() {
    if(count >= length / 3) {
      shrinkMaybe();
      return;
    }
    final int l = length;
    final Object[] oldList = list;
    length = count;
    shrinkMaybe();
    lowestFree = 0;
    for(int i = 0; i < l; ++i) {
      if(list[i] == null) {
        continue;
      }
      list[lowestFree] = oldList[i];
      ++lowestFree;
    }
    Arrays.fill(list, lowestFree, list.length, null);
  }

  protected void expectModCount(final int expectedModCount) {
    if(modCount != expectedModCount) throw new ConcurrentModificationException();
  }

  public void add(final T t) {
    final int expectedModCount = ++modCount;
    ensureSize(lowestFree);
    if(lowestFree >= length) {
      length = lowestFree + 1;
    }
    list[lowestFree] = Objects.requireNonNull(t);
    ++count;
    while(lowestFree < list.length && list[lowestFree] != null) {
      ++lowestFree;
    }
    expectModCount(expectedModCount);
  }

  public void remove(final T t) {
    final int expectedModCount = ++modCount;
    for(int i = 0; i < length; ++i) {
      if(t == list[i]) {
        list[i] = null;
        --count;
        // removed highest object?
        if(i == length - 1) {
          while(length >= 0 && list[length] == null) {
            --length;
          }
          if(list[length] != null) {
            ++length;
          }
          compact();
        }
        // removed lower than first empty?
        if(i < lowestFree) {
          lowestFree = i;
        }
      }
    }
    expectModCount(expectedModCount);
  }

  @Override
  public Iterator<T> iterator() {
    final int expectedModCount = modCount;
    final Object[] list = this.list;
    final int length = this.length;
    return new Iterator<T>() {

      private int i = -1;

      private void fillNext() {
        ++i;
        while(i < length && list[i] == null) {
          ++i;
        }
      }

      {
        fillNext();
      }

      @Override
      public boolean hasNext() {
        return i < length;
      }

      @Override
      public T next() {
        if(!hasNext()) throw new NoSuchElementException();
        @SuppressWarnings("unchecked")
        final T res = (T) list[i];
        fillNext();
        expectModCount(expectedModCount);
        return res;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  public boolean isEmpty() {
    return count == 0;
  }

  public int size() {
    return count;
  }

}
