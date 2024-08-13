/*-
 * ========================LICENSE_START=================================
 * jnb-buildable
 * %%
 * Copyright (C) 2023 - 2024 Eric Medvet
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

import io.github.ericmedvet.jnb.core.*;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Grid;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

@Discoverable(prefixTemplate = "misc|m")
public class Miscs {

  private Miscs() {}

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
  @Cacheable
  public static DoubleRange range(@Param("min") double min, @Param("max") double max) {
    return new DoubleRange(min, max);
  }

  @SuppressWarnings("unused")
  public static <T> Supplier<T> supplier(
      @Param("of") T target,
      @Param(value = "", injection = Param.Injection.MAP) ParamMap map,
      @Param(value = "", injection = Param.Injection.BUILDER) NamedBuilder<?> builder) {
    //noinspection unchecked
    return () -> (T) builder.build((NamedParamMap) map.value("of", ParamMap.Type.NAMED_PARAM_MAP));
  }
}
