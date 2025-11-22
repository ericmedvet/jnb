/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Interpolator;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.Naming;
import io.github.ericmedvet.jnb.datastructure.Utils;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Discoverable(prefixTemplate = "consumer")
public class Consumers {

  private Consumers() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, K, O> BiConsumer<X, K> composed(
      @Param(value = "f", dNPM = "f.identity()") Function<X, O> innerF,
      @Param(value = "consumer") BiConsumer<O, K> consumer
  ) {
    return Naming.named(
        "%s[f=%s]".formatted(consumer, NamedFunction.name(innerF)),
        (BiConsumer<X, K>) (x, k) -> consumer.accept(innerF.apply(x), k)
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static BiConsumer<?, ?> deaf() {
    return Naming.named("deaf", (i1, i2) -> {
    });
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, K, O> BiConsumer<X, K> saver(
      @Param(value = "of", dNPM = "f.identity()") Function<X, O> f,
      @Param(value = "overwrite") boolean overwrite,
      @Param("path") String filePathTemplate,
      @Param(value = "suffix", dS = "") String suffix,
      @Param("verbose") boolean verbose
  ) {
    return Naming.named(
        "saver[%s;%s]".formatted(
            NamedFunction.name(f),
            filePathTemplate + (overwrite ? "(*)" : "")
        ),
        (x, k) -> {
          Utils.save(f.apply(x), Interpolator.interpolate(filePathTemplate, k) + suffix, overwrite, verbose);
        }
    );
  }

}
