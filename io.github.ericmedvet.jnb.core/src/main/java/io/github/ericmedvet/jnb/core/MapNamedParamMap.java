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

import io.github.ericmedvet.jnb.core.parsing.TokenType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MapNamedParamMap implements NamedParamMap, Formattable {

  private final String name;
  private final SortedMap<String, Object> values;
  private final Map<String, SequencedSet<Type>> types;

  public MapNamedParamMap(String name, ParamMap paramMap) {
    this(
        name,
        (paramMap instanceof MapNamedParamMap) ? ((MapNamedParamMap) paramMap).values : paramMap.names()
            .stream()
            .collect(
                Collectors.toMap(
                    n -> n,
                    paramMap::value
                )
            )
    );
  }

  public MapNamedParamMap(String name, Map<String, Object> values) {
    this.name = name;
    this.values = Collections.unmodifiableSortedMap(new TreeMap<>(values));
    types = new HashMap<>();
    values.forEach((n, v) -> types.put(n, typesFor(v)));
    for (String n : types.keySet()) {
      if (types.get(n).isEmpty()) {
        throw new IllegalArgumentException(
            "Unsupported type for value %s of type %s".formatted(
                values.get(n),
                values.get(n).getClass().getSimpleName()
            )
        );
      }
    }
  }

  private static Boolean booleanValue(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof String s) {
      if (!s.equals("true") && !s.equals("false")) {
        return null;
      }
      return Boolean.valueOf(s);
    }
    return null;
  }


  private static int currentLineLength(String s) {
    String[] lines = s.split("\n");
    return lines[lines.length - 1].length();
  }

  private static <E extends Enum<E>> E enumValue(Object o, Class<E> enumClass) {
    if (o == null) {
      return null;
    }
    if (o instanceof String s) {
      return Enum.valueOf(enumClass, s.toUpperCase());
    }
    return null;
  }

  private static String getOrInterpolate(Object value, ParamMap paramMap) {
    return switch (value) {
      case InterpolableString interpolableString -> interpolableString.interpolate(paramMap);
      default -> value.toString();
    };
  }

  private static String indent(int w) {
    return IntStream.range(0, w).mapToObj(i -> " ").collect(Collectors.joining());
  }

  private static boolean isBoolean(Object o) {
    if (o instanceof String s) {
      return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
    }
    return false;
  }

  private static boolean isInt(Object o) {
    if (o instanceof Double d) {
      return !Double.isNaN(d) && !Double.isInfinite(d) && (int) d.doubleValue() == d;
    }
    return false;
  }

  private static boolean isInt(Double v) {
    return v.intValue() == v;
  }

  private static String listContentToInlineString(List<?> l, String space) {
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < l.size(); j++) {
      if (l.get(j) instanceof ParamMap m) {
        if (m instanceof NamedParamMap namedParamMap) {
          sb.append(namedParamMap.getName()).append(TokenType.OPEN_CONTENT.rendered());
        }
        sb.append(mapContentToInlineString(m, space));
        if (m instanceof NamedParamMap) {
          sb.append(TokenType.CLOSED_CONTENT.rendered());
        }
      } else if (l.get(j) instanceof String s) {
        sb.append(stringValue(s));
      } else if (l.get(j) instanceof Enum<?> e) {
        sb.append(e.name().toLowerCase());
      } else {
        sb.append(l.get(j).toString());
      }
      if (j < l.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered()).append(space);
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
        sb.append(TokenType.LIST_SEPARATOR.rendered());
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
          .append(TokenType.ASSIGN_SEPARATOR.rendered())
          .append(space);
      Object value = m.value(names.get(i));
      switch (value) {
        case List<?> l -> sb.append(TokenType.OPEN_LIST.rendered())
            .append(listContentToInlineString(l, space))
            .append(TokenType.CLOSED_LIST.rendered());
        case ParamMap innerMap -> {
          if (innerMap instanceof NamedParamMap namedParamMap) {
            sb.append(namedParamMap.getName()).append(TokenType.OPEN_CONTENT.rendered());
          }
          sb.append(mapContentToInlineString(innerMap, space));
          if (innerMap instanceof NamedParamMap) {
            sb.append(TokenType.CLOSED_CONTENT.rendered());
          }
        }
        case String s -> sb.append(stringValue(s));
        case null -> sb.append((String) null);
        default -> sb.append(value);
      }
      if (i < names.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered()).append(space);
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
          .append(TokenType.ASSIGN_SEPARATOR.rendered())
          .append(space);
      Object value = map.value(names.get(i));
      switch (value) {
        case List<?> l -> {
          sb.append(TokenType.OPEN_LIST.rendered());
          String listContent = listContentToInlineString(l, space);
          if (l.isEmpty() || listContent.length() + currentLineLength(sb.toString()) < maxW) {
            sb.append(listContent);
          } else {
            listContentToMultilineString(sb, maxW, w, indent, space, l);
          }
          sb.append(TokenType.CLOSED_LIST.rendered());
        }
        case NamedParamMap m -> prettyToString(m, sb, maxW, w + indent, indent, space);
        case String s -> sb.append(stringValue(s));
        case null -> sb.append((String) null);
        default -> sb.append(value);
      }
      if (i < names.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered());
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

  public static void prettyToString(
      ParamMap map,
      StringBuilder sb,
      int maxW,
      int w,
      int indent,
      String space
  ) {
    // iterate
    if (map instanceof NamedParamMap namedParamMap) {
      sb.append(namedParamMap.getName());
    }
    sb.append(TokenType.OPEN_CONTENT.rendered());
    String content = mapContentToInlineString(map, space);
    if (map.names().isEmpty() || content.length() + currentLineLength(sb.toString()) < maxW) {
      sb.append(content);
    } else {
      mapContentToMultilineString(sb, maxW, w, indent, space, map);
    }
    sb.append(TokenType.CLOSED_CONTENT.rendered());
  }

  @SafeVarargs
  private static <T> SequencedSet<T> set(T... ts) {
    LinkedHashSet<T> set = Arrays.stream(ts)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return Collections.unmodifiableSequencedSet(set);
  }

  private static String stringValue(String value) {
    return value.matches("[A-Za-z][A-Za-z0-9_]*") ? value : ('"' + value + '"');
  }

  private static SequencedSet<Type> typesFor(Object object) {
    return switch (object) {
      case Double d ->
        isInt(d) ? set(Type.INT, Type.DOUBLE, Type.STRING) : set(Type.DOUBLE, Type.STRING);
      case Integer i -> set(Type.INT, Type.DOUBLE, Type.STRING);
      case String s ->
        isBoolean(s) ? set(Type.STRING, Type.BOOLEAN, Type.ENUM) : set(Type.STRING, Type.ENUM);
      case InterpolableString is -> set(Type.STRING);
      case NamedParamMap npm -> set(Type.NAMED_PARAM_MAP);
      case List<?> list -> {
        if (list.isEmpty()) {
          yield set(
              Type.INTS,
              Type.DOUBLES,
              Type.STRINGS,
              Type.ENUMS,
              Type.BOOLEANS,
              Type.NAMED_PARAM_MAPS
          );
        }
        Object first = list.getFirst();
        yield switch (first) {
          case Double d -> {
            if (list.stream().allMatch(MapNamedParamMap::isInt)) {
              yield set(Type.INTS, Type.DOUBLES, Type.STRINGS);
            }
            if (list.stream().allMatch(o -> o instanceof Double)) {
              yield set(Type.DOUBLES, Type.STRINGS);
            }
            yield set(Type.DOUBLES, Type.STRINGS);
          }
          case Integer i -> {
            if (list.stream().allMatch(o -> o instanceof Integer)) {
              yield set(Type.INTS, Type.DOUBLES, Type.STRINGS);
            }
            yield set(Type.INTS, Type.DOUBLES, Type.STRINGS);
          }
          case String s -> {
            if (list.stream().allMatch(MapNamedParamMap::isBoolean)) {
              yield set(Type.STRINGS, Type.BOOLEANS, Type.ENUMS);
            }
            if (list.stream()
                .allMatch(o -> o instanceof String || o instanceof InterpolableString)) {
              yield set(Type.STRINGS, Type.ENUMS);
            }
            yield set();
          }
          case NamedParamMap npm -> {
            if (list.stream().allMatch(o -> o instanceof NamedParamMap)) {
              yield set(Type.NAMED_PARAM_MAPS);
            }
            yield set();
          }
          default -> set();
        };
      }
      default -> set();
    };
  }

  private Map<String, Object> allValues() {
    return names().stream()
        .collect(
            Collectors.toMap(
                n -> n,
                this::value
            )
        );
  }

  @Override
  public NamedParamMap and(ParamMap other) {
    Map<String, Object> newValues = new HashMap<>();
    if (other instanceof MapNamedParamMap mnpm) {
      newValues.putAll(mnpm.values);
    } else {
      other.names().forEach(n -> newValues.put(n, other.value(n)));
    }
    newValues.putAll(values);
    return new MapNamedParamMap(getName(), newValues);
  }

  @Override
  public NamedParamMap andOverwrite(ParamMap other) {
    Map<String, Object> newValues = new HashMap<>(values);
    if (other instanceof MapNamedParamMap mnpm) {
      newValues.putAll(mnpm.values);
    } else {
      other.names().forEach(n -> newValues.put(n, other.value(n)));
    }
    return new MapNamedParamMap(getName(), newValues);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MapNamedParamMap that)) {
      return false;
    }
    return Objects.equals(getName(), that.getName()) && Objects.equals(
        allValues(),
        that.allValues()
    );
  }

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    boolean alternate = (flags & FormattableFlags.ALTERNATE) == FormattableFlags.ALTERNATE;
    if (alternate) {
      formatter.format("%s", name);
    } else {
      formatter.format("%s", prettyToString(this, Integer.MAX_VALUE));
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), allValues());
  }

  @Override
  public Set<String> names() {
    return values.keySet();
  }

  @Override
  public String toString() {
    return prettyToString(this, Integer.MAX_VALUE);
  }

  @Override
  public SequencedSet<Type> types(String name) {
    return types.get(name);
  }

  @Override
  public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
    if (!types(name).contains(type)) {
      return null;
    }
    return switch (type) {
      case INT -> switch (values.get(name)) {
        case Double d -> d.intValue();
        case Integer i -> i;
        default -> null;
      };
      case DOUBLE -> switch (values.get(name)) {
        case Double d -> d;
        case Integer i -> i.doubleValue();
        default -> null;
      };
      case NAMED_PARAM_MAP, NAMED_PARAM_MAPS -> values.get(name);
      case BOOLEAN -> Boolean.parseBoolean((String) values.get(name));
      case ENUM -> enumValue(values.get(name), enumClass);
      case STRING -> getOrInterpolate(values.get(name), this);
      case INTS -> ((List<?>) values.get(name)).stream()
          .map(o -> switch (o) {
            case Double d -> d.intValue();
            case Integer i -> i;
            default -> null;
          })
          .toList();
      case DOUBLES -> ((List<?>) values.get(name)).stream()
          .map(o -> switch (o) {
            case Double d -> d;
            case Integer i -> i.doubleValue();
            default -> null;
          })
          .toList();
      case BOOLEANS ->
        ((List<?>) values.get(name)).stream()
            .map(o -> Boolean.parseBoolean(o.toString()))
            .toList();
      case ENUMS ->
        ((List<?>) values.get(name)).stream().map(o -> enumValue(o, enumClass)).toList();
      case STRINGS ->
        ((List<?>) values.get(name)).stream().map(o -> getOrInterpolate(o, this)).toList();
    };
  }

  @Override
  public NamedParamMap without(String... names) {
    Map<String, Object> newValues = new HashMap<>(values);
    for (String name : names) {
      newValues.remove(name);
    }
    return new MapNamedParamMap(getName(), newValues);
  }
}
