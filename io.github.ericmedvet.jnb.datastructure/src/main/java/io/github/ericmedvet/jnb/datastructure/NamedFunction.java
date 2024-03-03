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

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface NamedFunction<T, R> extends Function<T, R> {

  String UNNAMED_NAME = "unnamed";
  String NAME_JOINER = "→";

  static String composeNames(String... names) {
    return Arrays.stream(names).filter(s -> !s.equals(UNNAMED_NAME)).collect(Collectors.joining(NAME_JOINER));
  }

  static <T, R> NamedFunction<T, R> from(Function<T, R> f, String name) {
    return new NamedFunction<>() {
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

  static <T, R> NamedFunction<T, R> from(Function<T, R> f) {
    if (f instanceof NamedFunction<T, R> nf) {
      return nf;
    }
    return from(f, UNNAMED_NAME);
  }

  static String name(Function<?, ?> f) {
    if (f instanceof NamedFunction<?, ?> nf) {
      return nf.name();
    }
    return UNNAMED_NAME;
  }

  String name();

  default NamedFunction<T, R> renamed(String name) {
    return from(this, name);
  }

  @Override
  default <V> NamedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
    if (after instanceof FormattedFunction<? super R, ? extends V> afterFF) {
      return FormattedNamedFunction.from(
          t -> after.apply(apply(t)), afterFF.format(), composeNames(name(), name(after)));
    }
    return from(t -> after.apply(apply(t)), composeNames(name(), name(after)));
  }

  @Override
  default <V> NamedFunction<V, R> compose(Function<? super V, ? extends T> before) {
    return from(v -> apply(before.apply(v)), composeNames(name(before), name()));
  }
}
