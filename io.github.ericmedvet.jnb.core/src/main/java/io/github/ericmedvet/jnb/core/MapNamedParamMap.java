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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/// A [NamedParamMap] that internally stores its parameter names and values using a [SortedMap]. The
/// parameter names are returned by [ParamMap#names()] in lexicographical order.
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
        this.values.put(
            new TypedName(e.getKey().name, Type.DOUBLE),
            Double.valueOf(intValue(e.getValue()))
        );
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

  private static <E extends Enum<E>> E enumValue(Object o, Class<E> enumClass) {
    if (o == null) {
      return null;
    }
    if (o instanceof String s) {
      return Enum.valueOf(enumClass, s.toUpperCase());
    }
    return null;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MapNamedParamMap that)) {
      return false;
    }
    return Objects.equals(mapName(), that.mapName()) && Objects.equals(
        getValues(),
        that.getValues()
    );
  }

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    boolean alternate = (flags & FormattableFlags.ALTERNATE) == FormattableFlags.ALTERNATE;
    if (alternate) {
      formatter.format("%s", name);
    } else {
      formatter.format("%s", ParamMap.prettyToString(this, Integer.MAX_VALUE));
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
    return ParamMap.prettyToString(this, Integer.MAX_VALUE);
  }

  @Override
  public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
    return switch (type) {
      case INT -> intValue(values.get(new TypedName(name, Type.DOUBLE)));
      case BOOLEAN -> booleanValue(values.get(new TypedName(name, Type.STRING)));
      case ENUM -> enumValue(values.get(new TypedName(name, Type.STRING)), enumClass);
      case INTS -> checkList(
          (List<?>) values.get(new TypedName(name, Type.DOUBLES)),
          MapNamedParamMap::intValue
      );
      case BOOLEANS -> checkList(
          (List<?>) values.get(new TypedName(name, Type.STRINGS)),
          MapNamedParamMap::booleanValue
      );
      case ENUMS -> checkList(
          (List<?>) values.get(new TypedName(name, Type.STRINGS)),
          s -> enumValue(s, enumClass)
      );
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
