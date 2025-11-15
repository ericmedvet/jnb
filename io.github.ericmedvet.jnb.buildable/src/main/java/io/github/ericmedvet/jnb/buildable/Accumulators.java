package io.github.ericmedvet.jnb.buildable;

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.AccumulatorFactory;
import java.util.List;
import java.util.function.Function;

@Discoverable(prefixTemplate = "accumulator|acc")
public class Accumulators {

  private Accumulators() {
  }

  @SuppressWarnings("unused")
  public static <E, F, O, K> AccumulatorFactory<E, O, K> all(
      @Param(value = "eFunction", dNPM = "f.identity()") Function<E, F> eFunction,
      @Param(value = "listFunction", dNPM = "f.identity()") Function<List<F>, O> listFunction
  ) {
    return AccumulatorFactory.<E, F, K>collector(eFunction).then(listFunction);
  }

  @SuppressWarnings("unused")
  public static <E, O, K> AccumulatorFactory<E, O, K> first(
      @Param(value = "function", dNPM = "f.identity()") Function<E, O> function
  ) {
    return AccumulatorFactory.first((e, k) -> function.apply(e));
  }

  @SuppressWarnings("unused")
  public static <E, O, K> AccumulatorFactory<E, O, K> last(
      @Param(value = "function", dNPM = "f.identity()") Function<E, O> function
  ) {
    return AccumulatorFactory.last((e, k) -> function.apply(e));
  }

}
