/*
 * Copyright 2022 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jnb.core;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author "Eric Medvet" on 2022/08/08 for 2d-robot-evolution
 */
public class MapNamedParamMap implements NamedParamMap {

  private final String name;
  private final SortedMap<String, Double> dMap;
  private final SortedMap<String, String> sMap;
  private final SortedMap<String, NamedParamMap> npmMap;
  private final SortedMap<String, List<Double>> dsMap;
  private final SortedMap<String, List<String>> ssMap;
  private final SortedMap<String, List<NamedParamMap>> npmsMap;

  public MapNamedParamMap(
      String name,
      Map<String, Double> dMap,
      Map<String, String> sMap,
      Map<String, NamedParamMap> npmMap,
      Map<String, List<Double>> dsMap,
      Map<String, List<String>> ssMap,
      Map<String, List<NamedParamMap>> npmsMap
  ) {
    this.name = name;
    this.dMap = new TreeMap<>(dMap);
    this.sMap = new TreeMap<>(sMap);
    this.npmMap = new TreeMap<>(npmMap);
    this.dsMap = new TreeMap<>(dsMap);
    this.ssMap = new TreeMap<>(ssMap);
    this.npmsMap = new TreeMap<>(npmsMap);
  }

  private static int currentLineLength(String s) {
    String[] lines = s.split("\n");
    return lines[lines.length - 1].length();
  }

  private static String indent(int w) {
    return IntStream.range(0, w).mapToObj(i -> " ").collect(Collectors.joining());
  }

  private static boolean isInt(Double v) {
    return v.intValue() == v;
  }

  private static String listContentToInlineString(List<?> l, String space) {
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < l.size(); j++) {
      if (l.get(j) instanceof ParamMap m) {
        if (m instanceof NamedParamMap namedParamMap) {
          sb.append(namedParamMap.getName())
              .append(StringParser.TokenType.OPEN_CONTENT.rendered());
        }
        sb.append(mapContentToInlineString(m, space));
        if (m instanceof NamedParamMap) {
          sb.append(StringParser.TokenType.CLOSED_CONTENT.rendered());
        }
      } else if (l.get(j) instanceof String s) {
        sb.append(stringValue(s));
      } else if (l.get(j) instanceof Enum<?> e) {
        sb.append(e.name().toLowerCase());
      } else {
        sb.append(l.get(j).toString());
      }
      if (j < l.size() - 1) {
        sb.append(StringParser.TokenType.LIST_SEPARATOR.rendered()).append(space);
      }
    }
    return sb.toString();
  }

  private static void listContentToMultilineString(
      StringBuilder sb,
      int maxW,
      int w,
      int indent,
      String space,
      List<?> l
  ) {
    for (int j = 0; j < l.size(); j++) {
      sb.append("\n").append(indent(w + indent + indent));
      if (l.get(j) instanceof NamedParamMap m) {
        prettyToString(m, sb, maxW, w + indent + indent, indent, space);
      } else if (l.get(j) instanceof String s) {
        sb.append(stringValue(s));
      } else if (l.get(j) instanceof Enum<?> e) {
        sb.append(e.name().toLowerCase());
      } else {
        sb.append(l.get(j).toString());
      }
      if (j < l.size() - 1) {
        sb.append(StringParser.TokenType.LIST_SEPARATOR.rendered());
      }
    }
    sb.append("\n").append(indent(w + indent));
  }

  private static String mapContentToInlineString(ParamMap m, String space) {
    StringBuilder sb = new StringBuilder();
    List<String> names = new ArrayList<>(m.names());
    for (int i = 0; i < names.size(); i++) {
      sb.append(names.get(i))
          .append(space)
          .append(StringParser.TokenType.ASSIGN_SEPARATOR.rendered())
          .append(space);
      Object value = m.value(names.get(i));
      if (value instanceof List<?> l) {
        sb.append(StringParser.TokenType.OPEN_LIST.rendered())
            .append(listContentToInlineString(l, space))
            .append(StringParser.TokenType.CLOSED_LIST.rendered());
      } else if (value instanceof ParamMap innerMap) {
        if (innerMap instanceof NamedParamMap namedParamMap) {
          sb.append(namedParamMap.getName())
              .append(StringParser.TokenType.OPEN_CONTENT.rendered());
        }
        sb.append(mapContentToInlineString(innerMap, space));
        if (innerMap instanceof NamedParamMap) {
          sb.append(StringParser.TokenType.CLOSED_CONTENT.rendered());
        }
      } else if (value instanceof String) {
        sb.append(stringValue((String) value));
      } else {
        sb.append(value.toString());
      }
      if (i < names.size() - 1) {
        sb.append(StringParser.TokenType.LIST_SEPARATOR.rendered()).append(space);
      }
    }
    return sb.toString();
  }

  private static void mapContentToMultilineString(
      StringBuilder sb,
      int maxW,
      int w,
      int indent,
      String space,
      ParamMap map
  ) {
    List<String> names = new ArrayList<>(map.names());
    for (int i = 0; i < names.size(); i++) {
      sb.append("\n")
          .append(indent(w + indent))
          .append(names.get(i))
          .append(space)
          .append(StringParser.TokenType.ASSIGN_SEPARATOR.rendered())
          .append(space);
      Object value = map.value(names.get(i));
      if (value instanceof List<?> l) {
        sb.append(StringParser.TokenType.OPEN_LIST.rendered());
        String listContent = listContentToInlineString(l, space);
        if (l.isEmpty() || listContent.length() + currentLineLength(sb.toString()) < maxW) {
          sb.append(listContent);
        } else {
          listContentToMultilineString(sb, maxW, w, indent, space, l);
        }
        sb.append(StringParser.TokenType.CLOSED_LIST.rendered());
      } else if (value instanceof NamedParamMap m) {
        prettyToString(m, sb, maxW, w + indent, indent, space);
      } else if (value instanceof String) {
        sb.append(stringValue((String) value));
      } else {
        sb.append(value.toString());
      }
      if (i < names.size() - 1) {
        sb.append(StringParser.TokenType.LIST_SEPARATOR.rendered());
      }
    }
    sb.append("\n").append(indent(w));
  }

  @SuppressWarnings("unused")
  public static String prettyToString(ParamMap map) {
    return prettyToString(map, 80);
  }

  public static String prettyToString(ParamMap map, int maxW) {
    StringBuilder sb = new StringBuilder();
    prettyToString(map, sb, maxW, 0, 2, " ");
    return sb.toString();
  }

  public static void prettyToString(ParamMap map, StringBuilder sb, int maxW, int w, int indent, String space) {
    //iterate
    if (map instanceof NamedParamMap namedParamMap) {
      sb.append(namedParamMap.getName());
    }
    sb.append(StringParser.TokenType.OPEN_CONTENT.rendered());
    String content = mapContentToInlineString(map, space);
    if (map.names().isEmpty() || content.length() + currentLineLength(sb.toString()) < maxW) {
      sb.append(content);
    } else {
      mapContentToMultilineString(sb, maxW, w, indent, space, map);
    }
    sb.append(StringParser.TokenType.CLOSED_CONTENT.rendered());
  }

  @Override
  public Boolean b(String n) {
    if (sMap.containsKey(n) &&
        (sMap.get(n).equalsIgnoreCase(Boolean.TRUE.toString()) ||
            sMap.get(n).equalsIgnoreCase(Boolean.FALSE.toString()))) {
      return sMap.get(n).equalsIgnoreCase(Boolean.TRUE.toString());
    }
    return null;
  }

  private static String stringValue(String value) {
    return value.matches("[A-Za-z][A-Za-z0-9_]*") ? value : ('"' + value + '"');
  }

  @Override
  public Double d(String n) {
    return dMap.get(n);
  }

  @Override
  public List<Double> ds(String n) {
    return dsMap.get(n);
  }

  @Override
  public Integer i(String n) {
    if (!dMap.containsKey(n)) {
      return null;
    }
    double v = dMap.get(n);
    return isInt(v) ? (int) v : null;
  }

  @Override
  public List<Integer> is(String n) {
    if (!dsMap.containsKey(n)) {
      return null;
    }
    List<Double> vs = dsMap.get(n);
    List<Integer> is = vs.stream().filter(MapNamedParamMap::isInt).map(Double::intValue).toList();
    if (is.size() == vs.size()) {
      return is;
    }
    return null;
  }

  @Override
  public Set<String> names() {
    Set<String> names = new TreeSet<>();
    names.addAll(dMap.keySet());
    names.addAll(sMap.keySet());
    names.addAll(npmMap.keySet());
    names.addAll(dsMap.keySet());
    names.addAll(ssMap.keySet());
    names.addAll(npmsMap.keySet());
    return names;
  }

  @Override
  public NamedParamMap npm(String n) {
    return npmMap.get(n);
  }

  @Override
  public List<NamedParamMap> npms(String n) {
    return npmsMap.containsKey(n) ? npmsMap.get(n).stream().toList() : null;
  }

  @Override
  public String s(String n) {
    return sMap.get(n);
  }

  @Override
  public List<String> ss(String n) {
    return ssMap.get(n);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Boolean> bs(String n) {
    if (!ssMap.containsKey(n) || !ssMap.get(n)
        .stream()
        .allMatch(s -> s.equalsIgnoreCase(Boolean.TRUE.toString()) || s.equalsIgnoreCase(Boolean.FALSE.toString()))) {
      return null;
    }
    return ssMap.get(n).stream().map(s -> s.equalsIgnoreCase(Boolean.TRUE.toString())).toList();
  }

  @Override
  public String toString() {
    return prettyToString(this, Integer.MAX_VALUE);
  }
}
