package trails;

import jkanvas.Canvas;
import jkanvas.animation.AnimatedPainter;
import jkanvas.examples.ExampleUtil;

public class Main {

  public static void main(final String[] args) {
    final AnimatedPainter p = new AnimatedPainter();
    p.setFramerate(30);
    final Canvas c = new Canvas(p, true, 1024, 768);
    ExampleUtil.setupCanvas("Trails", c, p, true, true, true);
    p.addPass(new TrailRenderpass(p, 500, 500));
    c.reset();
  }

}
