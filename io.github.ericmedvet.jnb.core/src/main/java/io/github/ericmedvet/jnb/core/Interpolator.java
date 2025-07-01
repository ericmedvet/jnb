/*-
 * ========================LICENSE_START=================================
 * jnb-core
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
/// More precisely, `name` must match the pattern {@value MAP_KEYS_REGEX} and `format` must match {@value FORMAT_REGEX}.
public class Interpolator {

  private static final Logger L = Logger.getLogger(Interpolator.class.getName());
  private static final String FORMAT_REGEX = "%#?\\d*(\\.\\d+)?[sdf]";
  private static final String MAP_KEYS_REGEX = "[A-Za-z][A-Za-z0-9_]*";
  private static final Pattern INTERPOLATOR = Pattern.compile(
      "\\{(?<mapKeys>" + MAP_KEYS_REGEX + "(\\." + MAP_KEYS_REGEX + ")*)" + "(:(?<format>" + FORMAT_REGEX + "))?\\}"
  );

  private final String format;
  private final Matcher matcher;

  /// Constructs an `Interpolator` with the given template string. The template string can contain
  /// placeholders for interpolation.
  ///
  /// @param template the template string, potentially containing placeholders like `{name}` or
  ///                 `{name:template}`
  public Interpolator(String template) {
    this.format = template;
    matcher = INTERPOLATOR.matcher(template);
  }

  private static Object getKeyFromParamMap(ParamMap paramMap, List<String> keyPieces) {
    if (keyPieces.size() == 1) {
      return paramMap.value(keyPieces.getFirst());
    }
    ParamMap innerParamMap = (ParamMap) paramMap.value(
        keyPieces.getFirst(),
        ParamMap.Type.NAMED_PARAM_MAP
    );
    if (innerParamMap == null) {
      return null;
    }
    return getKeyFromParamMap(innerParamMap, keyPieces.subList(1, keyPieces.size()));
  }

  /// Interpolates the given `template` with the given `map`. If a `name` used in a placeholder is
  /// not in the `map`, the `noValueDefault` string is used to interpolate that placeholder. If a
  /// placeholder cannot be interpolated, the `noValueDefault` is used; if the latter is null, a
  /// `RuntimeException` is thrown.
  ///
  /// @param template       the template string, potentially containing placeholders like `{name}`
  ///                       or `{name:template}`
  /// @param map            the `ParamMap` containing the values for the interpolation
  /// @param noValueDefault the string to be used if a `name` is used in a placeholder but is not in
  ///                       the `map`
  /// @return the interpolated string
  public static String interpolate(String template, ParamMap map, String noValueDefault) {
    return new Interpolator(template).interpolate(map, noValueDefault);
  }

  /// Interpolates the given `template` with the given `map`. If a `name` used in a placeholder is
  /// not in the `map` or if the placeholder cannot be interpolated, a `RuntimeException` is
  /// thrown.
  ///
  /// @param template the template string, potentially containing placeholders like `{name}` or
  ///                 `{name:template}`
  /// @param map      the `ParamMap` containing the values for the interpolation
  /// @return the interpolated string
  public static String interpolate(String template, ParamMap map) {
    return new Interpolator(template).interpolate(map);
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
        Object v = getKeyFromParamMap(map, Arrays.stream(mapKeys.split("\\.")).toList());
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
