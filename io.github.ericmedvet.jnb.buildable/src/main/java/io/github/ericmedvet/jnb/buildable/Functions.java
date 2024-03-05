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

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

@Discoverable(prefixTemplate = "function|f")
public class Functions {

  private static final Logger L = Logger.getLogger(Functions.class.getName());

  private Functions() {}

  public static <X, Y> FormattedNamedFunction<X, Y> avg(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, List<? extends Number>> beforeF,
      @Param(value = "afterF", dNPM = "f.identity()") Function<Double, Y> afterF,
      @Param(value = "format", dS = "%.1f") String format) {
    Function<List<? extends Number>, Double> f =
        vs -> vs.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
    return FormattedNamedFunction.from(beforeF, f, afterF, format, "avg");
  }

  public static <X, Y> NamedFunction<X, Y> base64(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Object> beforeF,
      @Param(value = "afterF", dNPM = "f.identity()") Function<String, Y> afterF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Object, String> f = x -> {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(x);
        oos.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
      } catch (Throwable t) {
        L.warning("Cannot serialize  due to %s".formatted(t));
        return "not-serializable";
      }
    };
    return FormattedNamedFunction.from(beforeF, f, afterF, format, "base64");
  }

  public static <X> Function<X, X> identity() {
    return x -> x;
  }
}
