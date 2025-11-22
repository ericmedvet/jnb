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

  public static String interpolate(String format, ParamMap m, String noValueDefault) {
    return new Interpolator(format).interpolate(m, noValueDefault);
  }

  public static String interpolate(String format, ParamMap m) {
    return new Interpolator(format).interpolate(m);
  }

  public static String interpolate(String format, Object o) {
    return switch (o) {
      case ParamMap paramMap -> interpolate(format, paramMap);
      case Mappable mappable -> interpolate(format, mappable.map());
      case null, default -> format;
    };
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
