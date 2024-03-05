/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
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
package io.github.ericmedvet.jnb.datastructure;

import java.util.function.Function;

public interface FormattedFunction<T, R> extends Function<T, R> {

  String UNFORMATTED_FORMAT = "%s";

  static String format(Function<?, ?> f) {
    if (f instanceof FormattedFunction<?, ?> ff) {
      return ff.format();
    }
    return UNFORMATTED_FORMAT;
  }

  static <T, R> FormattedFunction<T, R> from(Function<T, R> f, String format) {
    return new FormattedFunction<>() {
      @Override
      public String format() {
        return format;
      }

      @Override
      public R apply(T t) {
        return f.apply(t);
      }

      @Override
      public String toString() {
        return "'%s'".formatted(format);
      }
    };
  }

  static <X, I, O, Y> FormattedFunction<X, Y> from(
      Function<X, I> beforeF, Function<I, O> f, Function<O, Y> afterF, String format) {
    return FormattedFunction.from(f, format)
        .compose(beforeF)
        .andThen(afterF)
        .reformatted(format);
  }

  static <T, R> FormattedFunction<T, R> from(Function<T, R> f) {
    if (f instanceof FormattedFunction<T, R> ff) {
      return ff;
    }
    return from(f, UNFORMATTED_FORMAT);
  }

  String format();

  default String applyFormatted(T t) {
    return format().formatted(apply(t));
  }

  default FormattedFunction<T, R> reformatted(String format) {
    return from(this, format);
  }

  @Override
  default <V> FormattedFunction<V, R> compose(Function<? super V, ? extends T> before) {
    if (before instanceof NamedFunction<? super V, ? extends T> beforeNF) {
      return FormattedNamedFunction.from(
          v -> apply(before.apply(v)),
          format(),
          NamedFunction.composeNames(beforeNF.name(), NamedFunction.name(this)));
    }
    return from(v -> apply(before.apply(v)), format());
  }

  @Override
  default <V> FormattedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
    if (after instanceof NamedFunction<? super R, ? extends V> afterNF) {
      return FormattedNamedFunction.from(
          t -> after.apply(apply(t)),
          format(after),
          NamedFunction.composeNames(NamedFunction.name(this), afterNF.name()));
    }
    return from(t -> after.apply(apply(t)), format(after));
  }
}
