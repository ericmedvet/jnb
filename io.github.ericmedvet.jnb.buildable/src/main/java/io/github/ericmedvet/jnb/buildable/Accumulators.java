/*-
 * ========================LICENSE_START=================================
 * jnb-buildable
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
