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

import java.util.Collection;
import java.util.function.Function;

/// A function which can format its output according to a format.
/// This function works like [Function], but provides a format in the form of a [String] that follows the format
/// string syntax used by [java.io.PrintStream#printf(String, Object...)].
///
/// @param <T> the type of the input of the function
/// @param <R> the type of the output of the function
public interface FormattedFunction<T, R> extends Function<T, R> {

  /// The default format to use when a format is not provided.
  String UNFORMATTED_FORMAT = "%s";

  /// Returns the format of this formatted function as a string formatted according to the format string syntax used
  /// by [java.io.PrintStream#printf(String, Object...)].
  ///
  /// @return the format of this formatted function
  String format();

  /// Returns the format of a function.
  /// If the function is a `FormattedFunction`, returns the result of [FormattedFunction#format()]; otherwise, return
  ///  [FormattedFunction#UNFORMATTED_FORMAT].
  ///
  /// @param f the function
  /// @return the format of the function
  static String format(Function<?, ?> f) {
    if (f instanceof FormattedFunction<?, ?> ff) {
      return ff.format();
    }
    return UNFORMATTED_FORMAT;
  }

  /// Builds a formatted function given a function and a format.
  ///
  /// @param f      the function
  /// @param format the format
  /// @param <T>    the type of the input of the function
  /// @param <R>    the type of the output of the function
  /// @return the formatted function
  static <T, R> FormattedFunction<T, R> from(Function<T, R> f, String format) {
    return new FormattedFunction<>() {
      @Override
      public R apply(T t) {
        return f.apply(t);
      }

      @Override
      public String format() {
        return format;
      }

      @Override
      public String toString() {
        return "'%s'".formatted(format);
      }
    };
  }

  /// Builds a formatted function given a function, possibly with the default format.
  /// If the input function `f` is a `FormattedFunction`, simply returns `f`; otherwise, calls
  ///  [FormattedFunction#from(Function, String)] with the default format [FormattedFunction#UNFORMATTED_FORMAT] as
  /// format.
  ///
  /// @param f   the function
  /// @param <T> the type of the input of the function
  /// @param <R> the type of the output of the function
  /// @return the named function
  static <T, R> FormattedFunction<T, R> from(Function<T, R> f) {
    if (f instanceof FormattedFunction<T, R> ff) {
      return ff;
    }
    return from(f, UNFORMATTED_FORMAT);
  }

  /// Applies this function to the given argument and format the result using the format.
  /// Formatting is performed through [String#formatted(Object...)].
  ///
  /// @param t the function argument
  /// @return the function result formatted using the format
  default String applyFormatted(T t) {
    return format().formatted(apply(t));
  }

  /// Returns a composed formatted function that first applies the `before` function to its input, and then applies this
  /// formatted function to the result.
  /// The format of the returned formatted function is the same of this function.
  ///
  /// @param before the function to apply before this function is applied
  /// @param <V>    the type of input of the `before` function, and of the composed function
  /// @return the composed formatted function
  @Override
  default <V> FormattedFunction<V, R> compose(Function<? super V, ? extends T> before) {
    if (before instanceof NamedFunction<? super V, ? extends T> beforeNF) {
      return FormattedNamedFunction.from(
          v -> apply(before.apply(v)),
          format(),
          NamedFunction.composeNames(beforeNF.name(), NamedFunction.name(this))
      );
    }
    return from(v -> apply(before.apply(v)), format());
  }

  /// Returns a composed formatted function that first applies this formatted function to its input, and then applies
  ///  the `after` function to the result.
  /// If the `after` function is a formatted function, the format of the returned function is the one of `after`;
  /// otherwise, it is the default format [FormattedFunction#UNFORMATTED_FORMAT].
  ///
  /// @param after the function to apply after this function is applied
  /// @param <V>   the type of output of the `after` function, and of the composed function
  /// @return the composed formatted function
  @Override
  default <V> FormattedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
    if (after instanceof NamedFunction<? super R, ? extends V> afterNF) {
      return FormattedNamedFunction.from(
          t -> after.apply(apply(t)),
          format(after),
          NamedFunction.composeNames(NamedFunction.name(this), afterNF.name())
      );
    }
    return from(t -> after.apply(apply(t)), format(after));
  }

  /// Returns a formatted function operating as this function but with the format set to the provided format.
  ///
  /// @param format the new format
  /// @return the reformatted named function
  default FormattedFunction<T, R> reformatted(String format) {
    return from(this, format);
  }

  /// Returns a formatted function operating as this function but with the format set to the shortest fixed length
  /// string-based format `%s` which completely show the string representation of each one of the provided input
  /// arguments.
  /// If this function format is not `%s`, returns this formatted function.
  /// Otherwise, applies with format the function to each `t` in `ts`, takes note of the resulting string length, and
  ///  sets the format of the returned function to `%`$n$`s`, where $n$ is the largest length.
  ///
  /// @param ts the input arguments to fit the format to
  /// @return the reformatted named function
  default FormattedFunction<T, R> reformattedToFit(Collection<? extends T> ts) {
    if (!format().equals("%s")) {
      return this;
    }
    int maxLength = ts.stream().mapToInt(t -> applyFormatted(t).length()).max().orElseThrow();
    return reformatted("%" + maxLength + "s");
  }
}
