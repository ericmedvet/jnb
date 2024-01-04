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

import io.github.ericmedvet.jnb.core.Param.Injection;
import io.github.ericmedvet.jnb.core.parsing.StringParser;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interpolator {

  private static final Logger L = Logger.getLogger(Interpolator.class.getName());
  private static final String FORMAT_REGEX = "%#?\\d*(\\.\\d+)?[sdf]";
  private static final String MAP_KEYS_REGEX = "[A-Za-z][A-Za-z0-9_]*";
  private static final Pattern INTERPOLATOR = Pattern.compile("\\{(?<mapKeys>"
      + MAP_KEYS_REGEX
      + "(\\."
      + MAP_KEYS_REGEX
      + ")*)"
      + "(:(?<format>"
      + FORMAT_REGEX
      + "))?\\}");

  private final String format;
  private final Matcher matcher;

  public Interpolator(String format) {
    this.format = format;
    matcher = INTERPOLATOR.matcher(format);
  }

  private static Object getKeyFromParamMap(ParamMap paramMap, List<String> keyPieces) {
    if (keyPieces.size() == 1) {
      return paramMap.value(keyPieces.get(0));
    }
    NamedParamMap namedParamMap = (NamedParamMap) paramMap.value(keyPieces.get(0), ParamMap.Type.NAMED_PARAM_MAP);
    if (namedParamMap == null) {
      return null;
    }
    return getKeyFromParamMap(namedParamMap, keyPieces.subList(1, keyPieces.size()));
  }

  public static String interpolate(String format, ParamMap m, String noValueDefault) {
    return new Interpolator(format).interpolate(m, noValueDefault);
  }

  public static String interpolate(String format, ParamMap m) {
    return new Interpolator(format).interpolate(m);
  }

  public String interpolate(ParamMap map) {
    return interpolate(map, null);
  }

  public String interpolate(ParamMap map, String noValueDefault) {
    StringBuilder sb = new StringBuilder();
    int c = 0;
    while (matcher.find(c)) {
      sb.append(format, c, matcher.start());
      try {
        String mapKeys = matcher.group("mapKeys");
        String f = matcher.group("format") != null ? matcher.group("format") : "%s";
        Object v = getKeyFromParamMap(
            map, Arrays.stream(mapKeys.split("\\.")).toList());
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
