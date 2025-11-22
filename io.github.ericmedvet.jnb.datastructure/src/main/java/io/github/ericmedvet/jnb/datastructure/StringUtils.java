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

public class StringUtils {

  public static final char VARIATION_UP = '↗';
  public static final char VARIATION_DOWN = '↘';
  public static final char VARIATION_SAME = '=';
  private static final String COLLAPSER_REGEX = "[.→\\[\\]]+";
  private static final String COLLAPSED_PIECE_REGEX = "\\w";

  private StringUtils() {
  }

  public static String collapse(String name) {
    StringBuilder acronym = new StringBuilder();
    String[] pieces = name.split(COLLAPSER_REGEX);
    for (String piece : pieces) {
      Matcher matcher = Pattern.compile(COLLAPSED_PIECE_REGEX).matcher(piece);
      if (matcher.find()) {
        acronym.append(piece, matcher.start(), matcher.end());
      }
    }
    return acronym.toString();
  }

  public static int formatSize(String format) {
    try {
      return format.formatted(0).length();
    } catch (IllegalFormatException e) {
      // ignore
    }
    try {
      return format.formatted(0.1).length();
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

  public static String justify(String s, int length) {
    if (s.length() > length) {
      return s.substring(0, length);
    }
    StringBuilder sBuilder = new StringBuilder(s);
    while (sBuilder.length() < length) {
      sBuilder.insert(0, " ");
    }
    s = sBuilder.toString();
    return s;
  }

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
