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

public interface FormattedNamedFunction<T, R> extends NamedFunction<T, R>, FormattedFunction<T, R> {

  static <T, R> FormattedNamedFunction<T, R> from(Function<T, R> f, String format, String name) {
    return new FormattedNamedFunction<>() {
      @Override
      public String format() {
        return format;
      }

      @Override
      public String name() {
        return name;
      }

      @Override
      public R apply(T t) {
        return f.apply(t);
      }
    };
  }

  static <X, I, O, Y> FormattedNamedFunction<X, Y> from(
      Function<X, I> beforeF, Function<I, O> f, Function<O, Y> afterF, String format, String name) {
    return FormattedNamedFunction.from(f, format, name)
        .compose(beforeF)
        .andThen(afterF)
        .reformatted(format);
  }

  static <T, R> FormattedNamedFunction<T, R> from(Function<T, R> f) {
    return from(f, FormattedFunction.format(f), NamedFunction.name(f));
  }

  @Override
  default FormattedNamedFunction<T, R> reformatted(String format) {
    return from(this, format, name());
  }

  @Override
  default NamedFunction<T, R> renamed(String name) {
    return from(this, format(), name);
  }

  @Override
  default <V> FormattedNamedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
    return from(
        t -> after.apply(apply(t)),
        FormattedFunction.format(after),
        NamedFunction.composeNames(name(), NamedFunction.name(after)));
  }

  @Override
  default <V> FormattedNamedFunction<V, R> compose(Function<? super V, ? extends T> before) {
    return from(
        v -> apply(before.apply(v)), format(), NamedFunction.composeNames(NamedFunction.name(before), name()));
  }
}
