/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
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
package io.github.ericmedvet.jnb.datastructure;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/// A function with a name.
/// This function works like [Function], but provides a name in the form of a [String].
///
/// @param <T> the type of the input of the function
/// @param <R> the type of the output of the function
public interface NamedFunction<T, R> extends Function<T, R> {

  /// The default name to use when a name is not provided.
  String UNNAMED_NAME = "unnamed";
  /// The name for the identity `NamedFunction`.
  String IDENTITY_NAME = "identity";
  /// The string used as joiner when doing the composition of two `NamedFunction` objects.
  String NAME_JOINER = "→";

  /// Returns the name of this named function.
  ///
  /// @return the name of this named function
  String name();

  /// Returns a string which is the concatenation of an array of strings taken as names.
  /// Takes the strings in `names` which are not the name of the unnamed or the identity functions and joins them
  /// with [NamedFunction#NAME_JOINER].
  ///
  /// @param names the strings to join
  /// @return the concatenation of the input names
  static String composeNames(String... names) {
    return Arrays.stream(names)
        .filter(s -> !s.equals(UNNAMED_NAME) && !s.equals(IDENTITY_NAME))
        .collect(Collectors.joining(NAME_JOINER));
  }

  /// Builds a named function given a function and a name.
  ///
  /// @param f    the function
  /// @param name the name
  /// @param <T>  the type of the input of the function
  /// @param <R>  the type of the output of the function
  /// @return the named function
  static <T, R> NamedFunction<T, R> from(Function<T, R> f, String name) {
    return new NamedFunction<>() {
      @Override
      public R apply(T t) {
        return f.apply(t);
      }

      @Override
      public String name() {
        return name;
      }

      @Override
      public String toString() {
        return "%s".formatted(name);
      }
    };
  }

  /// Builds a named function given a function, possibly with a default unnamed name.
  /// If the input function `f` is a `NamedFunction`, simply returns `f`; otherwise, calls [NamedFunction#from(Function, String)] with the [NamedFunction#UNNAMED_NAME] as name.
  ///
  /// @param f   the function
  /// @param <T> the type of the input of the function
  /// @param <R> the type of the output of the function
  /// @return the named function
  static <T, R> NamedFunction<T, R> from(Function<T, R> f) {
    if (f instanceof NamedFunction<T, R> nf) {
      return nf;
    }
    return from(f, UNNAMED_NAME);
  }

  /// Returns the name of a function.
  /// If the function is a `NamedFunction`, returns the result of [NamedFunction#name()]; otherwise, returns
  ///  [NamedFunction#UNNAMED_NAME].
  ///
  /// @param f the function
  /// @return the name of the function
  static String name(Function<?, ?> f) {
    if (f instanceof NamedFunction<?, ?> nf) {
      return nf.name();
    }
    return UNNAMED_NAME;
  }

  /// Returns a composed named function that first applies the `before` function to its input, and then applies this
  /// named function to the result.
  /// If the `before` function is a named function, the name of the returned function is a composition of the two
  /// names, through [NamedFunction#composeNames(String...)].
  ///
  /// @param before the function to apply before this function is applied
  /// @param <V>    the type of input of the `before` function, and of the composed function
  /// @return the composed named function
  @Override
  default <V> NamedFunction<V, R> compose(Function<? super V, ? extends T> before) {
    return from(v -> apply(before.apply(v)), composeNames(name(before), name()));
  }

  /// Returns a composed named function that first applies this named function to its input, and then applies the `
  /// after` function to the result.
  /// If the `after` function is a named function, the name of the returned function is a composition of the two
  /// names, through [NamedFunction#composeNames(String...)].
  ///
  /// @param after the function to apply after this function is applied
  /// @param <V>   the type of output of the `after` function, and of the composed function
  /// @return the composed named function
  @Override
  default <V> NamedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
    if (after instanceof FormattedFunction<? super R, ? extends V> afterFF) {
      return FormattedNamedFunction.from(
          t -> after.apply(apply(t)),
          afterFF.format(),
          composeNames(name(), name(after))
      );
    }
    return from(t -> after.apply(apply(t)), composeNames(name(), name(after)));
  }

  /// Returns a named function operating as this function but renamed with the provided name.
  ///
  /// @param name the new name
  /// @return the renamed named function
  default NamedFunction<T, R> renamed(String name) {
    return from(this, name);
  }
}
