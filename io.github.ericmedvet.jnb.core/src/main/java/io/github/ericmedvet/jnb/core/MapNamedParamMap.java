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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/// A [NamedParamMap] that internally stores its parameter names and values using a [SortedMap]. The
/// parameter names are returned by [ParamMap#names()] in lexicographical order. This class also
/// provides additional static methods to produce human-friendly representations of [ParamMap]
/// objects.
public class MapNamedParamMap implements NamedParamMap, Formattable {

  private final String name;
  private final SortedMap<TypedName, Object> values;

  /// Constructs a named map given a name and a map of parameters which also includes parameter
  /// types.
  ///
  /// @param name   the name of the map
  /// @param values a map containing the parameter values keyed by their typed names
  public MapNamedParamMap(String name, Map<TypedName, Object> values) {
    this.name = name;
    this.values = new TreeMap<>();
    for (Map.Entry<TypedName, Object> e : values.entrySet()) {
      if (e.getKey().type.equals(Type.INT)) {
        this.values.put(new TypedName(e.getKey().name, Type.DOUBLE),
            Double.valueOf(intValue(e.getValue())));
      } else if (e.getKey().type.equals(Type.BOOLEAN)) {
        this.values.put(
            new TypedName(e.getKey().name, Type.STRING),
            booleanValue(e.getValue().toString()).toString()
        );
      } else if (e.getKey().type.equals(Type.ENUM)) {
        this.values.put(
            new TypedName(e.getKey().name, Type.STRING),
            ((Enum<?>) e.getValue()).name().toLowerCase()
        );
      } else if (e.getKey().type.equals(Type.INTS)) {
        this.values.put(
            new TypedName(e.getKey().name, Type.DOUBLES),
            checkList((List<?>) e.getValue(), MapNamedParamMap::intValue)
        );
      } else if (e.getKey().type.equals(Type.BOOLEANS)) {
        this.values.put(
            new TypedName(e.getKey().name, Type.STRINGS),
            checkList(
                (List<?>) e.getValue(),
                b -> booleanValue(b.toString())
                    .toString()
            )
        );
      } else if (e.getKey().type.equals(Type.ENUMS)) {
        this.values.put(
            new TypedName(e.getKey().name, Type.STRINGS),
            checkList(
                (List<?>) e.getValue(),
                v -> ((Enum<?>) v).name().toLowerCase()
            )
        );
      } else {
        this.values.put(e.getKey(), e.getValue());
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

  private static List<?> checkList(List<?> l, Function<?, ?> mapper) {
    if (l == null) {
      return null;
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) List<?> mappedL = l.stream()
        .map(i -> ((Function) mapper).apply(i))
        .toList();
    if (mappedL.stream().anyMatch(Objects::isNull)) {
      return null;
    }
    return mappedL;
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

  private static String indent(int w) {
    return IntStream.range(0, w).mapToObj(i -> " ").collect(Collectors.joining());
  }

  private static Integer intValue(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number d) {
      return d.intValue() != d.doubleValue() ? null : d.intValue();
    }
    return null;
  }

  private static boolean isInt(Double v) {
    return v.intValue() == v;
  }

  private static String listContentToInlineString(List<?> l, String space) {
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < l.size(); j++) {
      if (l.get(j) instanceof ParamMap m) {
        if (m instanceof NamedParamMap namedParamMap) {
          sb.append(namedParamMap.mapName()).append(TokenType.OPEN_CONTENT.rendered());
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
            sb.append(namedParamMap.mapName()).append(TokenType.OPEN_CONTENT.rendered());
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
  public static void prettyToString(ParamMap map, StringBuilder stringBuilder, int maxLineLength,
      int indentOffset,
      int indentSize,
      String indentToken) {
    // iterate
    if (map instanceof NamedParamMap namedParamMap) {
      stringBuilder.append(namedParamMap.mapName());
    }
    stringBuilder.append(TokenType.OPEN_CONTENT.rendered());
    String content = mapContentToInlineString(map, indentToken);
    if (map.names().isEmpty()
        || content.length() + currentLineLength(stringBuilder.toString()) < maxLineLength) {
      stringBuilder.append(content);
    } else {
      mapContentToMultilineString(stringBuilder, maxLineLength, indentOffset, indentSize,
          indentToken, map);
    }
    stringBuilder.append(TokenType.CLOSED_CONTENT.rendered());
  }

  private static String stringValue(String value) {
    return value.matches("[A-Za-z][A-Za-z0-9_]*") ? value : ('"' + value + '"');
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MapNamedParamMap that)) {
      return false;
    }
    return Objects.equals(mapName(), that.mapName()) && Objects.equals(getValues(),
        that.getValues());
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

  public SortedMap<TypedName, Object> getValues() {
    return values;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mapName(), getValues());
  }

  @Override
  public String mapName() {
    return name;
  }

  @Override
  public Set<String> names() {
    return values.keySet().stream().map(k -> k.name).collect(Collectors.toSet());
  }

  @Override
  public String toString() {
    return prettyToString(this, Integer.MAX_VALUE);
  }

  @Override
  public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
    return switch (type) {
      case INT -> intValue(values.get(new TypedName(name, Type.DOUBLE)));
      case BOOLEAN -> booleanValue(values.get(new TypedName(name, Type.STRING)));
      case ENUM -> enumValue(values.get(new TypedName(name, Type.STRING)), enumClass);
      case INTS -> checkList((List<?>) values.get(new TypedName(name, Type.DOUBLES)),
          MapNamedParamMap::intValue);
      case BOOLEANS -> checkList(
          (List<?>) values.get(new TypedName(name, Type.STRINGS)),
          MapNamedParamMap::booleanValue
      );
      case ENUMS -> checkList((List<?>) values.get(new TypedName(name, Type.STRINGS)),
          s -> enumValue(s, enumClass));
      default -> values.get(new TypedName(name, type));
    };
  }

  /// A pair of a parameter name and a type. Objects of this type have a natural order which is the
  /// lexicographical order of their names.
  ///
  /// @param name the name of the parameter
  /// @param type the type of the parameter
  public record TypedName(String name, Type type) implements Comparable<TypedName> {

    /// Compares this pair with the provided `other` pair. The outcome is the same of the comparison
    /// of this and `other` names.
    ///
    /// @param other the pair to be compared
    /// @return -1 if this pair name comes first, 1 if it comes after, 0 if it is the same of the
    ///  `other` name
    @Override
    public int compareTo(TypedName other) {
      return name.compareTo(other.name);
    }
  }
}
