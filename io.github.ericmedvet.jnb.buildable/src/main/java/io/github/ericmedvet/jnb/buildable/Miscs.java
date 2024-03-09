package io.github.ericmedvet.jnb.buildable;

import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Grid;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

public class Miscs {

  private Miscs() {
  }

  @SuppressWarnings("unused")
  public static RandomGenerator defaultRG(@Param(value = "seed", dI = 0) int seed) {
    return seed >= 0 ? new Random(seed) : new Random();
  }

  @SuppressWarnings("unused")
  public static <T> Grid<T> grid(@Param("w") int w, @Param("h") int h, @Param("items") List<T> items) {
    if (items.size() != w * h) {
      throw new IllegalArgumentException(
          "Wrong number of items: %d x %d = %d expected, %d found".formatted(w, h, w * h, items.size()));
    }
    return Grid.create(w, h, items);
  }

  @SuppressWarnings("unused")
  public static DoubleRange range(@Param("min") double min, @Param("max") double max) {
    return new DoubleRange(min, max);
  }

}
