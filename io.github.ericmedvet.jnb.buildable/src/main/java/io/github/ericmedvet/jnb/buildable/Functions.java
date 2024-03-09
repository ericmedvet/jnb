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
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@Discoverable(prefixTemplate = "function|f")
public class Functions {

  private static final Logger L = Logger.getLogger(Functions.class.getName());

  private Functions() {}

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> avg(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, List<? extends Number>> beforeF,
      @Param(value = "format", dS = "%.1f") String format) {
    Function<List<? extends Number>, Double> f =
        vs -> vs.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
    return FormattedNamedFunction.from(f, format, "avg").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> NamedFunction<X, String> toBase64(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Object> beforeF,
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
    return FormattedNamedFunction.from(f, format, "to.base64").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> Function<X, X> identity() {
    return x -> x;
  }

  @SuppressWarnings("unused")
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> max(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f =
        cs -> cs.stream().max(Comparable::compareTo).orElseThrow();
    return FormattedNamedFunction.from(f, format, "max").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> min(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f =
        cs -> cs.stream().min(Comparable::compareTo).orElseThrow();
    return FormattedNamedFunction.from(f, format, "min").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> percentile(
      @Param("p") double p,
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f = cs ->
        cs.stream().sorted().toList().get((int) Math.min(cs.size() - 1, Math.max(0, cs.size() * p / 100d)));
    return FormattedNamedFunction.from(f, format, "percentile[%02.0f]".formatted(p))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> median(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f =
        cs -> cs.stream().sorted().toList().get(Math.min(cs.size() - 1, Math.max(0, cs.size() / 2)));
    return FormattedNamedFunction.from(f, format, "median").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, T, R> NamedFunction<X, Collection<R>> each(
      @Param("mapF") Function<T, R> mapF,
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<T>> beforeF) {
    Function<Collection<T>, Collection<R>> f = ts -> ts.stream().map(mapF).toList();
    return NamedFunction.from(f, "each[%s]".formatted(NamedFunction.name(mapF)))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, T> FormattedNamedFunction<X, T> nTh(
      @Param("n") int n,
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, List<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<List<T>, T> f = ts -> ts.get(n);
    return FormattedNamedFunction.from(f, format, "[%d]".formatted(n)).compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, T> FormattedNamedFunction<X, List<T>> nkTh(
      @Param("n") int n,
      @Param("k") int k,
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, List<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<List<T>, List<T>> f = ts -> IntStream.range(0, ts.size())
        .filter(i -> (i % n) == k)
        .mapToObj(ts::get)
        .toList();
    return FormattedNamedFunction.from(f, format, "[%di+%d]".formatted(n, k))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, T> FormattedNamedFunction<X, List<T>> subList(
      @Param("from") double from,
      @Param("to") double to,
      @Param(value = "relative", dB = true) boolean relative,
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, List<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<List<T>, List<T>> f =
        ts -> ts.subList((int) Math.min(Math.max(0, relative ? (from * ts.size()) : from), ts.size()), (int)
            Math.min(Math.max(0, relative ? (to * ts.size()) : to), ts.size()));
    return FormattedNamedFunction.from(f, format, "sub[%s%f-%f]".formatted(relative ? "%" : "", from, to))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> quantized(
      @Param("q") double q,
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Number> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Number, Double> f = v -> q * Math.floor(v.doubleValue() / q + 0.5);
    return FormattedNamedFunction.from(f, format, "q[%.1f]".formatted(q)).compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Integer> size(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<?>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<?>, Integer> f = Collection::size;
    return FormattedNamedFunction.from(f, format, "size").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> uniqueness(
      @Param(value = "beforeF", dNPM = "f.identity()") Function<X, Collection<?>> beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Collection<?>, Double> f =
        ts -> (double) ts.stream().distinct().count() / (double) ts.size();
    return FormattedNamedFunction.from(f, format, "uniqueness").compose(beforeF);
  }
}
