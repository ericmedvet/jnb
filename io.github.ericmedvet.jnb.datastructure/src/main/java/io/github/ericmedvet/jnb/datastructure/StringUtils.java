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

import java.time.LocalDateTime;
import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// This class consist of `static` utility methods for operating with strings or producing strings.
public class StringUtils {

  /// The char to be shown for representing a positive variation (increasing value), set to
  /// {@value VARIATION_UP}.
  public static final char VARIATION_UP = '↗';
  /// The char to be shown for representing a negative variation (decreasing value), set to
  /// {@value VARIATION_DOWN}.
  public static final char VARIATION_DOWN = '↘';
  /// The char to be shown for representing no variation, set to {@value VARIATION_SAME}.
  public static final char VARIATION_SAME = '=';
  private static final String COLLAPSER_REGEX = "[.→\\[\\]]+";
  private static final String COLLAPSED_PIECE_REGEX = "\\w";

  private StringUtils() {
  }

  /// Collapses a given string `string` into an acronym. The method splits the `string` by the
  /// predefined regex {@value COLLAPSER_REGEX} and then extracts the first alphanumeric character
  /// from each resulting piece to form the acronym.
  ///
  /// @param string the string to be collapsed
  /// @return the collapsed acronym string
  public static String collapse(String string) {
    StringBuilder acronym = new StringBuilder();
    String[] pieces = string.split(COLLAPSER_REGEX);
    for (String piece : pieces) {
      Matcher matcher = Pattern.compile(COLLAPSED_PIECE_REGEX).matcher(piece);
      if (matcher.find()) {
        acronym.append(piece, matcher.start(), matcher.end());
      }
    }
    return acronym.toString();
  }

  /// Estimates the formatted size of a string based on a given format string. This method attempts
  /// to format various types (int, double, LocalDateTime, and string) with the provided `format`
  /// and returns the length of the resulting string.
  ///
  /// @param format the format string to estimate the size for
  /// @return the estimated length of the formatted string
  public static int formatSize(String format) {
    try {
      return format.formatted(-1).length();
    } catch (IllegalFormatException e) {
      // ignore
    }
    try {
      return format.formatted(-0.1).length();
    } catch (IllegalFormatException e) {
      // ignore
    }
    try {
      return format.formatted(LocalDateTime.now()).length();
    } catch (IllegalFormatException e) {
      // ignore
    }
    return format.formatted("").length();
  }

  /// Justifies to the right a string `string` to a specified `length` by prepending spaces if
  /// `string` is shorter than `length`. If `string` is longer than `length`, it truncates `string`
  /// to `length`.
  ///
  /// @param string the string to justify
  /// @param length the desired length of the justified string
  /// @return the justified string
  public static String justify(String string, int length) {
    if (string.length() > length) {
      return string.substring(0, length);
    }
    StringBuilder sBuilder = new StringBuilder(string);
    while (sBuilder.length() < length) {
      sBuilder.insert(0, " ");
    }
    string = sBuilder.toString();
    return string;
  }

  /// Determines the variation between two objects, `current` and `last`, assumed to be numbers. It
  /// returns `VARIATION_UP` if `current` is greater than `last`, `VARIATION_DOWN` if `current` is
  /// less than `last`, and `VARIATION_SAME` if they are equal. If either `current` or `last` is
  /// null, or if they are not instances of `Number`, it returns a space character.
  ///
  /// @param current the current value
  /// @param last    the last value
  /// @return a character representing the variation
  public static char variation(Object current, Object last) {
    if (current == null || last == null) {
      return ' ';
    }
    if (!(current instanceof Number) || !(last instanceof Number)) {
      return ' ';
    }
    double currentN = ((Number) current).doubleValue();
    double lastN = ((Number) last).doubleValue();
    if (currentN < lastN) {
      return VARIATION_DOWN;
    }
    if (currentN > lastN) {
      return VARIATION_UP;
    }
    return VARIATION_SAME;
  }
}
