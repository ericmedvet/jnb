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

/// A function with a name which can format its output according to a format.
/// This function works like [Function], but provides a name in the form of a [String] and a format in the form of a
/// [String] that follows the format string syntax used by [java.io.PrintStream#printf(String, Object...)].
///
/// @param <T> the type of the input of the function
/// @param <R> the type of the output of the function
public interface FormattedNamedFunction<T, R> extends NamedFunction<T, R>, FormattedFunction<T, R> {

  /// Builds a formatted and named function given a function, a name, and a format.
  ///
  /// @param f      the function
  /// @param format the format
  /// @param name   the name
  /// @param <T>    the type of the input of the function
  /// @param <R>    the type of the output of the function
  /// @return the formatted and named function
  static <T, R> FormattedNamedFunction<T, R> from(Function<T, R> f, String format, String name) {
    return new FormattedNamedFunction<>() {
      @Override
      public R apply(T t) {
        return f.apply(t);
      }

      @Override
      public String format() {
        return format;
      }

      @Override
      public String name() {
        return name;
      }

      @Override
      public String toString() {
        return "%s('%s')".formatted(name, format);
      }
    };
  }

  /// Builds a named and formatted function given a function, possibly with the default name and format.
  /// If the input function `f` is a `FormattedNamedFunction`, , simply returns `f`; otherwise, calls
  ///  [FormattedNamedFunction#from(Function, String, String)] retaining the name (if `f` is a `NamedFunction`) or
  /// the format (if `f` is a `FormattedFunction`).
  ///
  /// @param f   the function
  /// @param <T> the type of the input of the function
  /// @param <R> the type of the output of the function
  /// @return the formatted and named function
  static <T, R> FormattedNamedFunction<T, R> from(Function<T, R> f) {
    if (f instanceof FormattedNamedFunction<T, R> fnf) {
      return fnf;
    }
    return from(f, FormattedFunction.format(f), NamedFunction.name(f));
  }

  /// Returns a composed formatted and named function that first applies the `before` function to its input, and then
  ///  applies this named function to the result.
  /// The format of the returned formatted function is the same of this function.
  ///
  /// @param before the function to apply before this function is applied
  /// @param <V>    the type of input of the `before` function, and of the composed function
  /// @return the composed formatted and named function
  @Override
  default <V> FormattedNamedFunction<V, R> compose(Function<? super V, ? extends T> before) {
    return from(
        v -> apply(before.apply(v)),
        format(),
        NamedFunction.composeNames(NamedFunction.name(before), name())
    );
  }

  /// Returns a composed formatted and named function that first applies this formatted function to its input, and
  /// then applies the `after` function to the result.
  /// If the `after` function is a formatted function, the format of the returned function is the one of `after`;
  /// otherwise, it is the default format [FormattedFunction#UNFORMATTED_FORMAT].
  /// If the `after` function is a named function, the name of the returned function is a composition of the two
  /// names, through [NamedFunction#composeNames(String...)].
  ///
  /// @param after the function to apply after this function is applied
  /// @param <V>   the type of output of the `after` function, and of the composed function
  /// @return the composed formatted and named function
  @Override
  default <V> FormattedNamedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
    return from(
        t -> after.apply(apply(t)),
        FormattedFunction.format(after),
        NamedFunction.composeNames(name(), NamedFunction.name(after))
    );
  }

  /// Returns a formatted and named function operating as this function but renamed with the provided name.
  ///
  /// @param name the new name
  /// @return the renamed formatted and named function
  @Override
  default FormattedNamedFunction<T, R> renamed(String name) {
    return from(this, format(), name);
  }

  /// Returns a formatted and named function operating as this function but with the format set to the provided format.
  ///
  /// @param format the new format
  /// @return the reformatted formatted and named function
  @Override
  default FormattedNamedFunction<T, R> reformatted(String format) {
    return from(this, format, name());
  }
}
