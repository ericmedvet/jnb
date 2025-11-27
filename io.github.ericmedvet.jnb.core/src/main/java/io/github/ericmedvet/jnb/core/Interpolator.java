/*-
 * ========================LICENSE_START=================================
 * jnb-core
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
package io.github.ericmedvet.jnb.core;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// A class that can be used to interpolate templated strings with the contents of a `ParamMap`. The
/// typical usage is as follows. Assume a `ParamMap` with the following contents: `person.height` ->
/// `1.746`, `person.name` -> `Eric` The code:
/// ```java
/// String s = Interpolator.interpolate("The person is {person.name}({person.name:%.2f}m)", map);
/// ```
/// would result in `s` being `The person is Eric (1.75m)`.
///
/// The variable parts of the template, i.e., the data placeholders, have the format `{name:format}`,
/// where `name` is a string valid as a `ParamMap` name and `format` is a format usable by
/// [java.io.PrintStream#printf(String, Object...)].
/// More precisely, `name` must match the pattern <code>{@value MAP_KEYS_REGEX}</code> and `format` must match
/// <code>{@value FORMAT_REGEX}</code>.
public class Interpolator {

  private static final Logger L = Logger.getLogger(Interpolator.class.getName());
  private static final String PARENT_KEY = "^";
  private static final String ROOT_KEY = "^^";
  private static final String FORMAT_REGEX = "%#?\\d*(\\.\\d+)?[sdf]";
  private static final String MAP_KEY_REGEX = "[A-Za-z][A-Za-z0-9_]*";
  private static final String ARRAY_INDEX_REGEX = Pattern.quote("[") + "(-?[0-9]+)" + Pattern.quote("]");
  private static final String MAP_KEYS_REGEX = "(" + Pattern.quote(ROOT_KEY) + "|" + Pattern.quote(
      PARENT_KEY
  ) + "|" + MAP_KEY_REGEX + "|" + MAP_KEY_REGEX + ARRAY_INDEX_REGEX + ")(\\.(" + Pattern.quote(
      PARENT_KEY
  ) + "|" + MAP_KEY_REGEX + "|" + MAP_KEY_REGEX + ARRAY_INDEX_REGEX + "))*";
  private static final Pattern INTERPOLATOR = Pattern.compile(
      "\\{(?<mapKeys>" + MAP_KEYS_REGEX + ")" + "(:(?<format>" + FORMAT_REGEX + "))?\\}"
  );

  private final String format;
  private final Matcher matcher;

  /// Constructs an `Interpolator` with the given template string. The template string can contain
  /// placeholders for interpolation.
  ///
  /// @param format the template string, potentially containing placeholders like `{name}` or
  ///               `{name:template}`
  public Interpolator(String format) {
    this.format = format;
    matcher = INTERPOLATOR.matcher(format);
  }

  private static Object getKeyFromParamMap(ParamMap paramMap, List<String> keyPieces) {
    String key = keyPieces.getFirst();
    if (key.equals(PARENT_KEY)) {
      if (paramMap.parent() != null) {
        return getKeyFromParamMap(paramMap.parent(), keyPieces.subList(1, keyPieces.size()));
      }
    }
    if (key.equals(ROOT_KEY)) {
      ParamMap pm = paramMap;
      while (pm.parent() != null) {
        pm = pm.parent();
      }
      return getKeyFromParamMap(pm, keyPieces.subList(1, keyPieces.size()));
    }
    Object value;
    Matcher m = Pattern.compile(ARRAY_INDEX_REGEX).matcher(key);
    if (m.find()) {
      String indexString = m.group(1);
      int i = Integer.parseInt(indexString);
      Object listValue = paramMap.value(key.substring(0, m.start()));
      if (listValue instanceof List<?> list) {
        value = list.get(i < 0 ? (list.size() - i) : i);
      } else {
        return null;
      }
    } else {
      value = paramMap.value(keyPieces.getFirst());
    }
    if (value == null) {
      return null;
    }
    if (keyPieces.size() == 1) {
      return value;
    }
    return getKeyFromParamMap((ParamMap) value, keyPieces.subList(1, keyPieces.size()));
  }

  /// Interpolates the given `template` with the given `map`. If a `name` used in a placeholder is
  /// not in the `map`, the `noValueDefault` string is used to interpolate that placeholder. If a
  /// placeholder cannot be interpolated, the `noValueDefault` is used; if the latter is null, a
  /// `RuntimeException` is thrown.
  ///
  /// @param format         the template string, potentially containing placeholders like `{name}`
  ///                       or `{name:template}`
  /// @param map            the `ParamMap` containing the values for the interpolation
  /// @param noValueDefault the string to be used if a `name` is used in a placeholder but is not in
  ///                       the `map`
  /// @return the interpolated string
  public static String interpolate(String format, ParamMap map, String noValueDefault) {
    return new Interpolator(format).interpolate(map, noValueDefault);
  }

  /// Interpolates the given `template` with the given `map`. If a `name` used in a placeholder is
  /// not in the `map` or if the placeholder cannot be interpolated, a `RuntimeException` is
  /// thrown.
  ///
  /// @param format the template string, potentially containing placeholders like `{name}` or
  ///               `{name:template}`
  /// @param map    the `ParamMap` containing the values for the interpolation
  /// @return the interpolated string
  public static String interpolate(String format, ParamMap map) {
    return new Interpolator(format).interpolate(map);
  }

  /// Interpolates the given `template` with the given `object`. If the object is a `ParamMap`, this
  /// method calls [#interpolate(String, ParamMap)] after casting. If the object is a [Mappable],
  /// this method extracts the `ParamMap` from it and calls [#interpolate(String, ParamMap)].
  /// Otherwise, return the string `format` without interpolation.
  ///
  /// @param format the template string, potentially containing placeholders like `{name}` or
  ///               `{name:template}`
  /// @param object the object to use for interpolation
  /// @return the interpolated string
  public static String interpolate(String format, Object object) {
    return switch (object) {
      case ParamMap paramMap -> interpolate(format, paramMap);
      case Mappable mappable -> interpolate(format, mappable.map());
      case null, default -> format;
    };
  }

  /// Interpolates the template of this interpolator with the given `map`. If a `name` used in a
  /// placeholder is not in the `map` or if the placeholder cannot be interpolated, a
  /// `RuntimeException` is thrown.
  ///
  /// @param map the `ParamMap` containing the values for the interpolation
  /// @return the interpolated string
  public String interpolate(ParamMap map) {
    return interpolate(map, null);
  }

  /// Interpolates the template of this interpolator with the given `map`. If a `name` used in a
  /// placeholder is not in the `map`, the `noValueDefault` string is used to interpolate that
  /// placeholder. If a placeholder cannot be interpolated, the `noValueDefault` is used; if the
  /// latter is null, a `RuntimeException` is thrown.
  ///
  /// @param map            the `ParamMap` containing the values for the interpolation
  /// @param noValueDefault the string to be used if a `name` is used in a placeholder but is not in
  ///                       the `map`
  /// @return the interpolated string
  public String interpolate(ParamMap map, String noValueDefault) {
    StringBuilder sb = new StringBuilder();
    int c = 0;
    while (matcher.find(c)) {
      sb.append(format, c, matcher.start());
      try {
        String mapKeys = matcher.group("mapKeys");
        String f = matcher.group("format") != null ? matcher.group("format") : "%s";
        Object v = getKeyFromParamMap(
            map,
            Arrays.stream(mapKeys.split("\\.")).toList()
        );
        sb.append(f.formatted(v));
      } catch (RuntimeException e) {
        if (noValueDefault != null) {
          L.warning("Cannot interpolate name: %s".formatted(e));
          sb.append(noValueDefault);
        } else {
          throw e;
        }
      }
      c = matcher.end();
    }
    sb.append(format, c, format.length());
    return sb.toString();
  }
}