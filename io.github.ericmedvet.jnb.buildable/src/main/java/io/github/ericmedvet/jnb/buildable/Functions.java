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

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.MathOp;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Discoverable(prefixTemplate = "function|f")
public class Functions {

  private static final Logger L = Logger.getLogger(Functions.class.getName());

  private Functions() {}

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> avg(
      @Param(value = "of", dNPM = "f.identity()") Function<X, List<? extends Number>> beforeF,
      @Param(value = "format", dS = "%.1f") String format) {
    Function<List<? extends Number>, Double> f =
        vs -> vs.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
    return FormattedNamedFunction.from(f, format, "avg").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> NamedFunction<X, String> classSimpleName(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Object> beforeF) {
    Function<Object, String> f = o -> o.getClass().getSimpleName();
    return NamedFunction.from(f, "class").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> clip(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Double> beforeF,
      @Param("range") DoubleRange range,
      @Param(value = "format", dS = "%.1f") String format) {

    Function<Double, Double> f = range::clip;
    return FormattedNamedFunction.from(
            f, format, ("clip[" + format + ";" + format + "]").formatted(range.min(), range.max()))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, Z, Y> FormattedNamedFunction<X, Y> composition(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Z> beforeF,
      @Param(value = "then", dNPM = "f.identity()") Function<Z, Y> afterF) {
    return FormattedNamedFunction.from(afterF, FormattedFunction.format(afterF), NamedFunction.name(afterF))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> NamedFunction<X, Set<T>> distinct(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<T>, Set<T>> f = HashSet::new;
    return FormattedNamedFunction.from(f, format, "distinct").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T, R> NamedFunction<X, Collection<R>> each(
      @Param("mapF") Function<T, R> mapF,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<T>> beforeF) {
    Function<Collection<T>, Collection<R>> f = ts -> ts.stream().map(mapF).toList();
    return NamedFunction.from(f, "each[%s]".formatted(NamedFunction.name(mapF)))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> NamedFunction<X, Collection<T>> filter(
      @Param(value = "condition", dNPM = "predicate.always()") Predicate<T> condition,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<T>, Collection<T>> f =
        ts -> ts.stream().filter(condition).toList();
    return FormattedNamedFunction.from(f, format, "filter[%s]".formatted(condition))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> NamedFunction<X, Object> fromBase64(
      @Param(value = "of", dNPM = "f.identity()") Function<X, String> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<String, Object> f = s -> {
      try (ByteArrayInputStream bais =
              new ByteArrayInputStream(Base64.getDecoder().decode(s));
          ObjectInputStream ois = new ObjectInputStream(bais)) {
        return ois.readObject();
      } catch (Throwable t) {
        L.warning("Cannot deserialize due to %s".formatted(t));
        return null;
      }
    };
    return FormattedNamedFunction.from(f, format, "from.base64").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, Double> gridCompactness(
      @Param(value = "predicate", dNPM = "f.nonNull()") Function<T, Boolean> predicate,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<T>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<T>, Double> f = g -> GridUtils.compactness(g, predicate::apply);
    return FormattedNamedFunction.from(f, format, "grid.compactness").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, Integer> gridCount(
      @Param(value = "predicate", dNPM = "f.nonNull()") Function<T, Boolean> predicate,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<T>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<T>, Integer> f = g -> GridUtils.count(g, predicate::apply);
    return FormattedNamedFunction.from(f, format, "grid.count").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, Double> gridCoverage(
      @Param(value = "predicate", dNPM = "f.nonNull()") Function<T, Boolean> predicate,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<T>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<T>, Double> f = g -> (double) GridUtils.count(g, predicate::apply) / (double) (g.w() * g.h());
    return FormattedNamedFunction.from(f, format, "grid.coverage").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, Double> gridElongation(
      @Param(value = "predicate", dNPM = "f.nonNull()") Function<T, Boolean> predicate,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<T>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<T>, Double> f = g -> GridUtils.elongation(g, predicate::apply);
    return FormattedNamedFunction.from(f, format, "grid.elongation").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, Integer> gridFitH(
      @Param(value = "predicate", dNPM = "f.nonNull()") Function<T, Boolean> predicate,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<T>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<T>, Integer> f = g -> GridUtils.fit(g, predicate::apply).h();
    return FormattedNamedFunction.from(f, format, "grid.fit.h").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, Integer> gridFitW(
      @Param(value = "predicate", dNPM = "f.nonNull()") Function<T, Boolean> predicate,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<T>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<T>, Integer> f = g -> GridUtils.fit(g, predicate::apply).w();
    return FormattedNamedFunction.from(f, format, "grid.fit.w").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Integer> gridH(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<?>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<?>, Integer> f = Grid::h;
    return FormattedNamedFunction.from(f, format, "grid.h").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Integer> gridW(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Grid<?>> beforeF,
      @Param(value = "format", dS = "%2d") String format) {
    Function<Grid<?>, Integer> f = Grid::w;
    return FormattedNamedFunction.from(f, format, "grid.w").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Function<X, X> identity() {
    Function<X, X> f = x -> x;
    return NamedFunction.from(f, NamedFunction.IDENTITY_NAME);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> mathConst(
      @Param("v") double v, @Param(value = "format", dS = "%.1f") String format) {
    return FormattedNamedFunction.from(x -> v, format, format.formatted(v));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, Y> FormattedNamedFunction<X, Double> mathOp(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Y> beforeF,
      @Param("args") List<Function<Y, ? extends Number>> args,
      @Param("op") MathOp op,
      @Param(value = "format", dS = "%.1f") String format) {
    Function<Y, Double> f = y -> op.applyAsDouble(
        args.stream().mapToDouble(aF -> aF.apply(y).doubleValue()).toArray());
    return FormattedNamedFunction.from(
            f,
            format,
            "%s[%s]"
                .formatted(
                    op.toString().toLowerCase(),
                    args.stream().map(NamedFunction::name).collect(Collectors.joining(";"))))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> max(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f =
        cs -> cs.stream().max(Comparable::compareTo).orElseThrow();
    return FormattedNamedFunction.from(f, format, "max").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> median(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f =
        cs -> cs.stream().sorted().toList().get(Math.min(cs.size() - 1, Math.max(0, cs.size() / 2)));
    return FormattedNamedFunction.from(f, format, "median").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> min(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f =
        cs -> cs.stream().min(Comparable::compareTo).orElseThrow();
    return FormattedNamedFunction.from(f, format, "min").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> NamedFunction<X, T> nTh(
      @Param("n") int n,
      @Param(value = "of", dNPM = "f.identity()") Function<X, List<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<List<T>, T> f = ts -> n >= 0 ? ts.get(n) : ts.get(ts.size() + n);
    return FormattedNamedFunction.from(f, format, "[%d]".formatted(n)).compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, List<T>> nkTh(
      @Param("n") int n,
      @Param("k") int k,
      @Param(value = "of", dNPM = "f.identity()") Function<X, List<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<List<T>, List<T>> f = ts -> IntStream.range(0, ts.size())
        .filter(i -> (i % n) == k)
        .mapToObj(ts::get)
        .toList();
    return FormattedNamedFunction.from(f, format, "[%di+%d]".formatted(n, k))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Boolean> nonNull(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Object> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Object, Boolean> f = Objects::nonNull;
    return FormattedNamedFunction.from(f, format, "non.null").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, F, S> FormattedNamedFunction<X, F> pairFirst(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Pair<F, S>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Pair<F, S>, F> f = Pair::first;
    return FormattedNamedFunction.from(f, format, "first").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, F, S> FormattedNamedFunction<X, S> pairSecond(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Pair<F, S>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Pair<F, S>, S> f = Pair::second;
    return FormattedNamedFunction.from(f, format, "second").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, C extends Comparable<C>> FormattedNamedFunction<X, C> percentile(
      @Param("p") double p,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<C>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Collection<C>, C> f = cs ->
        cs.stream().sorted().toList().get((int) Math.min(cs.size() - 1, Math.max(0, cs.size() * p / 100d)));
    return FormattedNamedFunction.from(f, format, "percentile[%02.0f]".formatted(p))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> quantized(
      @Param("q") double q,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Number> beforeF,
      @Param(value = "format", dS = "%.1f") String format) {
    Function<Number, Double> f = v -> q * Math.floor(v.doubleValue() / q + 0.5);
    return FormattedNamedFunction.from(f, format, "q[%s]".formatted(format.formatted(q)))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> sd(
      @Param(value = "of", dNPM = "f.identity()") Function<X, List<? extends Number>> beforeF,
      @Param(value = "format", dS = "%.1f") String format) {
    Function<List<? extends Number>, Double> f = vs -> {
      double mean = vs.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
      return Math.sqrt(vs.stream()
              .mapToDouble(v -> v.doubleValue() - mean)
              .map(v -> v * v)
              .sum()
          / ((double) vs.size()));
    };
    return FormattedNamedFunction.from(f, format, "sd").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Integer> size(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<?>> beforeF,
      @Param(value = "format", dS = "%3d") String format) {
    Function<Collection<?>, Integer> f = Collection::size;
    return FormattedNamedFunction.from(f, format, "size").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> FormattedNamedFunction<X, List<T>> subList(
      @Param("from") double from,
      @Param("to") double to,
      @Param(value = "relative", dB = true) boolean relative,
      @Param(value = "of", dNPM = "f.identity()") Function<X, List<T>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<List<T>, List<T>> f =
        ts -> ts.subList((int) Math.min(Math.max(0, relative ? (from * ts.size()) : from), ts.size()), (int)
            Math.min(Math.max(0, relative ? (to * ts.size()) : to), ts.size()));
    return FormattedNamedFunction.from(f, format, "sub[%s%f-%f]".formatted(relative ? "%" : "", from, to))
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> NamedFunction<X, String> toBase64(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Object> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Object, String> f = x -> {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(x);
        oos.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
      } catch (Throwable t) {
        L.warning("Cannot serialize due to %s".formatted(t));
        return "not-serializable";
      }
    };
    return FormattedNamedFunction.from(f, format, "to.base64").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> NamedFunction<X, String> toString(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Object> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Object, String> f = Object::toString;
    return FormattedNamedFunction.from(f, format, "to.string").compose(beforeF);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> uniqueness(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Collection<?>> beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Collection<?>, Double> f =
        ts -> (double) ts.stream().distinct().count() / (double) ts.size();
    return FormattedNamedFunction.from(f, format, "uniqueness").compose(beforeF);
  }
}
