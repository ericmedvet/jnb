/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2024 Eric Medvet
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
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.FormattedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Discoverable(prefixTemplate = "predicate")
public class Predicates {
  private Predicates() {}

  private static <T> Predicate<T> named(Predicate<T> predicate, String name) {
    return new Predicate<>() {
      @Override
      public boolean test(T t) {
        return predicate.test(t);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> all(@Param("conditions") List<Predicate<X>> conditions) {
    return named(
        x -> conditions.stream().allMatch(p -> p.test(x)),
        "all[%s]".formatted(conditions.stream().map(Object::toString).collect(Collectors.joining(";"))));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Predicate<?> always() {
    return named(t -> true, "always");
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> any(@Param("conditions") List<Predicate<X>> conditions) {
    return named(
        x -> conditions.stream().anyMatch(p -> p.test(x)),
        "any[%s]".formatted(conditions.stream().map(Object::toString).collect(Collectors.joining(";"))));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, T> Predicate<X> eq(
      @Param(value = "f", dNPM = "f.identity()") Function<X, T> function, @Param("v") T v) {
    return named(x -> function.apply(x).equals(v), "%s==%s".formatted(NamedFunction.name(function), v));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> gt(
      @Param(value = "f", dNPM = "f.identity()") Function<X, ? extends Number> function, @Param("t") double t) {
    return named(
        x -> function.apply(x).doubleValue() > t,
        "%s>%s"
            .formatted(
                NamedFunction.name(function),
                FormattedFunction.format(function).formatted(t)));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> gtEq(
      @Param(value = "f", dNPM = "f.identity()") Function<X, ? extends Number> function, @Param("t") double t) {
    return named(
        x -> function.apply(x).doubleValue() >= t,
        "%s≥%s"
            .formatted(
                NamedFunction.name(function),
                FormattedFunction.format(function).formatted(t)));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> inD(
      @Param(value = "f", dNPM = "f.identity()") Function<X, Double> function,
      @Param("values") List<Double> values) {
    return named(
        x -> values.contains(function.apply(x)),
        "%s∈{%s}"
            .formatted(
                NamedFunction.name(function),
                values.stream()
                    .map(v -> FormattedFunction.format(function)
                        .formatted(v))
                    .collect(Collectors.joining(";"))));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> inI(
      @Param(value = "f", dNPM = "f.identity()") Function<X, Integer> function,
      @Param("values") List<Integer> values) {
    return named(
        x -> values.contains(function.apply(x)),
        "%s∈{%s}"
            .formatted(
                NamedFunction.name(function),
                values.stream()
                    .map(v -> FormattedFunction.format(function)
                        .formatted(v))
                    .collect(Collectors.joining(";"))));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> inL(
      @Param(value = "f", dNPM = "f.identity()") Function<X, Long> function,
      @Param("values") List<Integer> values) {
    return named(
        x -> values.contains(function.apply(x).intValue()),
        "%s∈{%s}"
            .formatted(
                NamedFunction.name(function),
                values.stream()
                    .map(v -> FormattedFunction.format(function)
                        .formatted(v))
                    .collect(Collectors.joining(";"))));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> inS(
      @Param(value = "f", dNPM = "f.identity()") Function<X, String> function,
      @Param("values") List<String> values) {
    return named(
        x -> values.contains(function.apply(x)),
        "%s∈{%s}"
            .formatted(
                NamedFunction.name(function),
                values.stream()
                    .map(v -> FormattedFunction.format(function)
                        .formatted(v))
                    .collect(Collectors.joining(";"))));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> lt(
      @Param(value = "f", dNPM = "f.identity()") Function<X, ? extends Number> function, @Param("t") double t) {
    return named(
        x -> function.apply(x).doubleValue() < t,
        "%s<%s"
            .formatted(
                NamedFunction.name(function),
                FormattedFunction.format(function).formatted(t)));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> ltEq(
      @Param(value = "f", dNPM = "f.identity()") Function<X, ? extends Number> function, @Param("t") double t) {
    return named(
        x -> function.apply(x).doubleValue() <= t,
        "%s≤%s"
            .formatted(
                NamedFunction.name(function),
                FormattedFunction.format(function).formatted(t)));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> matches(
      @Param(value = "f", dNPM = "f.identity()") Function<X, String> function, @Param("regex") String regex) {
    Pattern p = Pattern.compile(regex);
    return named(
        x -> p.matcher(function.apply(x)).matches(),
        "%s~%s"
            .formatted(
                NamedFunction.name(function),
                FormattedFunction.format(function).formatted(regex)));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> not(@Param("condition") Predicate<X> condition) {
    return named(condition.negate(), "¬%s".formatted(condition));
  }
}
