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

import io.github.ericmedvet.jnb.core.parsing.TokenType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/// A [NamedParamMap] that internally stores its parameter names and values using a [SortedMap]. The
/// parameter names are returned by [ParamMap#names()] in lexicographical order.
public class MapNamedParamMap implements NamedParamMap, Formattable {

  private final String name;
  private final SortedMap<String, Object> values;
  private final Map<String, SequencedSet<Type>> types;
  private ParamMap parent;

  // TODO write doc
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

  /// Constructs a named map given a name and a map of parameters which also includes parameter
  /// types.
  ///
  /// @param name   the name of the map
  /// @param values a map containing the parameter values keyed by their typed names
  public MapNamedParamMap(String name, Map<String, Object> values) {
    this(name, values, null);
  }

  // TODO write doc
  public MapNamedParamMap(String name, Map<String, Object> values, ParamMap parent) {
    this.name = name;
    this.values = Collections.unmodifiableSortedMap(new TreeMap<>(values));
    this.parent = parent;
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
    propagateParent(parent);
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
      case InterpolableString interpolableString -> {
        try {
          yield interpolableString.interpolate(paramMap);
        } catch (RuntimeException e) {
          yield "ERR:" + interpolableString;
        }
      }
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

  /// Returns a human-friendly string representation of the provided named `map`.
  ///
  /// @param map the map to be represented as string
  /// @return the string representation of the provided `map`
  @SuppressWarnings("unused")
  public static String prettyToString(ParamMap map) {
    return prettyToString(map, 80);
  }

  /// Returns a human-friendly string representation of the provided named `map`, possibly with
  /// indentation, depending on `maxLineLength`.
  ///
  /// Internally uses [#prettyToString(ParamMap, StringBuilder, int, int, int, String)], with a
  /// blank space as indentation token, an indentation size of 2 tokens, and no offset.
  ///
  /// @param map           the map to be represented as string
  /// @param maxLineLength the maximum line length in the string representation, impacting also on
  /// @return the string representation of the provided `map`
  public static String prettyToString(ParamMap map, int maxLineLength) {
    StringBuilder sb = new StringBuilder();
    prettyToString(map, sb, maxLineLength, 0, 2, " ");
    return sb.toString();
  }

  /// Produces a human-friendly string representation of the provided named `map` and puts it in the
  /// provided [StringBuilder]. The string representation will have reasonable indentation according
  /// to the parameters `maxLineLength`, `indentOffset`, `indentSize`, and `indentToken`. The string
  /// representation will be parsable by the
  /// [io.github.ericmedvet.jnb.core.parsing.StringParser#parse(String)] method: this method hence
  /// acts like a serialization method, while the mentioned `parse()` method acts like a
  /// deserialization method.
  ///
  /// If the provided map is a [NamedParamMap], also the name of the map is represented in the
  /// string.
  ///
  /// @param map           the map to be represented as string
  /// @param stringBuilder the string builder to direct the representation to
  /// @param maxLineLength the maximum line length in the string representation, impacting also on
  ///                      indentation style
  /// @param indentOffset  the offset for indentation (each line in the produced string
  ///                      representation will have `indentOffset` indentation tokens)
  /// @param indentSize    the number of indentation tokens to be used at each indentation
  /// @param indentToken   the indentation token (a blank indentToken being a reasonable value)
  public static void prettyToString(
      ParamMap map,
      StringBuilder stringBuilder,
      int maxLineLength,
      int indentOffset,
      int indentSize,
      String indentToken
  ) {
    // iterate
    if (map instanceof NamedParamMap namedParamMap) {
      stringBuilder.append(namedParamMap.getName());
    }
    stringBuilder.append(TokenType.OPEN_CONTENT.rendered());
    String content = mapContentToInlineString(map, indentToken);
    if (map.names().isEmpty() || content.length() + currentLineLength(stringBuilder.toString()) < maxLineLength) {
      stringBuilder.append(content);
    } else {
      mapContentToMultilineString(stringBuilder, maxLineLength, indentOffset, indentSize, indentToken, map);
    }
    stringBuilder.append(TokenType.CLOSED_CONTENT.rendered());
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
        isBoolean(s) ? set(Type.BOOLEAN, Type.STRING, Type.ENUM) : set(Type.STRING, Type.ENUM);
      case InterpolableString is -> set(Type.STRING);
      case Enum<?> e -> set(Type.STRING, Type.ENUM);
      case Boolean b -> set(Type.BOOLEAN, Type.STRING);
      case NamedParamMap npm -> set(Type.NAMED_PARAM_MAP, Type.STRING);
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
          case InterpolableString is -> set(Type.STRINGS, Type.BOOLEANS, Type.ENUMS);
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
          case Enum<?> e -> {
            if (list.stream()
                .allMatch(o -> o instanceof String || o instanceof Enum<?>)) {
              yield set(Type.STRINGS, Type.ENUMS);
            }
            yield set();
          }
          case Boolean b -> {
            if (list.stream()
                .allMatch(o -> o instanceof Boolean || isBoolean(o))) {
              yield set(Type.BOOLEANS, Type.STRINGS);
            }
            yield set();
          }
          case NamedParamMap npm -> {
            if (list.stream().allMatch(o -> o instanceof NamedParamMap)) {
              yield set(Type.NAMED_PARAM_MAPS, Type.STRINGS);
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
    return new MapNamedParamMap(getName(), newValues, parent);
  }

  @Override
  public NamedParamMap andOverwrite(ParamMap other) {
    Map<String, Object> newValues = new HashMap<>(values);
    if (other instanceof MapNamedParamMap mnpm) {
      newValues.putAll(mnpm.values);
    } else {
      other.names().forEach(n -> newValues.put(n, other.value(n)));
    }
    return new MapNamedParamMap(getName(), newValues, parent);
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
  public ParamMap parent() {
    return parent;
  }

  @Override
  public void propagateParent(ParamMap paramMap) {
    parent = paramMap;
    for (Object value : values.values()) {
      if (value instanceof ParamMap childParamMap) {
        childParamMap.propagateParent(this);
      }
      if (value instanceof List<?> list) {
        for (Object lValue : list) {
          if (lValue instanceof ParamMap childParamMap) {
            childParamMap.propagateParent(this);
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    return prettyToString(this, Integer.MAX_VALUE);
  }

  @Override
  public SequencedSet<Type> types(String name) {
    return types.getOrDefault(name, set());
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
      case BOOLEAN -> booleanValue(values.get(name));
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
      case BOOLEANS -> ((List<?>) values.get(name)).stream()
          .map(this::booleanValue)
          .toList();
      case ENUMS ->
        ((List<?>) values.get(name)).stream().map(o -> enumValue(o, enumClass)).toList();
      case STRINGS ->
        ((List<?>) values.get(name)).stream().map(o -> getOrInterpolate(o, this)).toList();
    };
  }

  private boolean booleanValue(Object o) {
    if (o instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(o.toString());
  }

  @Override
  public NamedParamMap without(String... names) {
    Map<String, Object> newValues = new HashMap<>(values);
    for (String name : names) {
      newValues.remove(name);
    }
    return new MapNamedParamMap(getName(), newValues, parent);
  }
}