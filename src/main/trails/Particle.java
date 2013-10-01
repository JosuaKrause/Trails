package trails;

import jkanvas.animation.AnimatedPosition;

public class Particle extends AnimatedPosition {

  private final double size;

  public Particle(final double x, final double y, final double size) {
    super(x, y);
    this.size = size;
  }

  public double getSize() {
    return size;
  }

}
