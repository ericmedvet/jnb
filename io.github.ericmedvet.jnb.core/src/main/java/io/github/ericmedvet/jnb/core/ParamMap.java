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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// An object mapping parameter names (strings) to parameter (typed) values.
/// Values are type by means of [Type], whose constants are associated with Java types.
/// Parameter values can be retrieved by name (with [#value(String)]) or by name and [Type] (with
/// [#value(String, Type)]): with the latter, the actual runtime Java type of the returned value is the one
/// associated with the provided `Type`.
/// Typically, instances of this interface may be obtained by parsing a string, through the
/// [io.github.ericmedvet.jnb.core.parsing.StringParser#parse(String)] map, or by obtaining modified views of an
/// existing `ParamMap`, e.g., through [#with(String, Type, Object)] and [#without(String...)] methods.
public interface ParamMap {

  /// The type of values that can be stored in a `ParamMap`.
  /// Besides the `enum` name, each `Type` has a short string representation obtainable with [#rendered()], useful
  /// for documenting and debugging builders in a `NamedBuilder`.
  /// Each constant is associated with a Java type, which is the actual one returned (if possible) by
  /// [#value(String, Type)].
  enum Type {
    /// Natural numbers, associated with `Integer`; the type is rendered `i`.
    INT("i"),
    /// Real numbers, associated with `Double`; the type is rendered `d`.
    DOUBLE("d"),
    /// Strings, associated with `String`; the type is rendered `s`.
    STRING("s"),
    /// Boolean values, associated with `Boolean`; the type is rendered `b`.
    BOOLEAN("b"),
    /// Enumerations, associated with an `enum`; the type is rendered `e`.
    /// Values of this type are constants of a specific `enum` and have to be retrieved through
    ///  [#value(String, Type, Class)].
    ENUM("e"),
    /// Named maps of named parameters, associated with [NamedParamMap]; the type is rendered `npm`.
    NAMED_PARAM_MAP("npm"),
    /// Lists of natural numbers, associated with `List<Integer>`; the type is rendered `i[]`.
    INTS("i[]"),
    /// Lists of real numbers, associated with `List<Double>`; the type is rendered `d[]`.
    DOUBLES("d[]"),
    /// Lists of strings, associated with `List<String>`; the type is rendered `s[]`.
    STRINGS("s[]"),
    /// Lists of Boolean values, associated with `List<Boolean>`; the type is rendered `b[]`.
    BOOLEANS("b[]"),
    /// Lists of enumerations, associated with `List<E>`, where `E` is a specific `enum`; the type is rendered `e[]`.
    ///
    /// @see Type#ENUM
    ENUMS("e[]"),
    /// Lists of named maps of named parameters, associated with `List<NamedParamMap>`; the type is rendered `npm[]`.
    NAMED_PARAM_MAPS("npm[]");

    private final String rendered;

    Type(String rendered) {
      this.rendered = rendered;
    }

    /// Returns a short string representation of this type.
    ///
    /// @return a short string representation of this type
    public String rendered() {
      return rendered;
    }

    /// Returns a short string representation of this type.
    ///
    /// @return a short string representation of this type
    @Override
    public String toString() {
      return rendered;
    }
  }

  /// Returns the set of all the names (in no specified order) of the parameters stored in this map.
  ///
  /// @return the names of the parameters of this map
  Set<String> names();

  /// Returns the value of the parameter stored in this map with the provided `name` and `type`, if any.
  /// The actual runtime Java type of the returned value is the one specified by `type`.
  /// If `type` is [Type#ENUM] or [Type#ENUMS], `enumClass` has to be specified, otherwise `enumClass` is ignored.
  ///
  /// If the map contains a parameter with the provided `name` but with a different type than `type`, this method
  /// will return `null`.
  ///
  /// @param name      the name of the parameter to get the value of
  /// @param type      the type of the parameter
  /// @param enumClass the (optional) class of the enumerated param value
  /// @param <E>       the type of the (optional) enumeration
  /// @return the value of the parameter with name `name` and type `type`, or `null` otherwise
  <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass);

  private static boolean areEquals(ParamMap pm1, ParamMap pm2) {
    if (!pm1.names().equals(pm2.names())) {
      return false;
    }
    for (String name : pm1.names()) {
      if (!Objects.equals(pm1.value(name), pm2.value(name))) {
        return false;
      }
    }
    return true;
  }

  private static int hash(ParamMap pm) {
    return Objects.hash(
        pm.names().stream().map(n -> Map.entry(n, pm.value(n))).toArray(Object[]::new)
    );
  }

  /// Returns a new `ParamMap` with all the parameters of this map and those of the provided `other` map.
  /// Hence, the set of names returned by `names()` is the union of the two sets.
  /// When retrieving a value, this map has the precedence over the `other` map.
  ///
  /// @param other the other `ParamMap` to get the values from
  /// @return a new `ParamMap` with all the parameters of this and the `other` maps
  default ParamMap and(ParamMap other) {
    ParamMap thisParamMap = this;
    return new ParamMap() {
      @Override
      public int hashCode() {
        return ParamMap.hash(thisParamMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof ParamMap otherMap) {
          return ParamMap.areEquals(this, otherMap);
        }
        return false;
      }

      @Override
      public Set<String> names() {
        return Stream.concat(thisParamMap.names().stream(), other.names().stream())
            .collect(Collectors.toSet());
      }

      @Override
      public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
        Object o = thisParamMap.value(name, type, enumClass);
        if (o == null) {
          o = other.value(name, type, enumClass);
        }
        return o;
      }
    };
  }

  /// Returns the value of the parameter stored in this map with the provided `name` and `type`, if any.
  /// Works for all types different from [Type#ENUM] and [Type#ENUMS].
  /// Internally calls [#value(String, Type, Class)].
  ///
  /// @param name the name of the parameter to get the value of
  /// @param type the type of the parameter
  /// @return the value of the parameter with name `name` and type `type`, or `null` otherwise
  /// @throws IllegalArgumentException if `type` is or [Type#ENUM] or [Type#ENUMS]
  default Object value(String name, Type type) {
    if (type.equals(Type.ENUM) || type.equals(Type.ENUMS)) {
      throw new IllegalArgumentException("Cannot obtain enum(s) type for \"%s\" without enum class".formatted(name));
    }
    return value(name, type, null);
  }

  /// Returns the value of the parameter stored in this map with the provided `name`, if any.
  /// If more than one types are compatible with the parameter value with this type (e.g., a natural number called `n
  ///` being compatible with both [Type#INT] and [Type#DOUBLE]), then the type which comes first in the order
  /// dictated by [Type] constants is chosen.
  /// Internally calls [#value(String, Type)].
  ///
  /// @param name the name of the parameter to get the value of
  /// @return the value of the parameter with name `name`, or `null` otherwise
  default Object value(String name) {
    return Arrays.stream(Type.values())
        .filter(t -> !(t.equals(Type.ENUM) || t.equals(Type.ENUMS)))
        .map(t -> value(name, t))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /// Returns a new `ParamMap` with all the parameters of this map and another parameter as provided.
  /// When retrieving a value, this map has the precedence over the provided parameter.
  ///
  /// @param newName  the name of the new parameter
  /// @param newType  the type of the value of the new parameter
  /// @param newValue the value of the new parameter
  /// @return a new `ParamMap` with all the parameters of this map and the new parameter
  default ParamMap with(String newName, Type newType, Object newValue) {
    ParamMap thisParamMap = this;
    return new ParamMap() {
      @Override
      public int hashCode() {
        return ParamMap.hash(thisParamMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof ParamMap other) {
          return ParamMap.areEquals(this, other);
        }
        return false;
      }

      @Override
      public Set<String> names() {
        return Stream.concat(thisParamMap.names().stream(), Stream.of(newName))
            .collect(Collectors.toSet());
      }

      @Override
      public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
        Object o = thisParamMap.value(name, type, enumClass);
        if (o == null && newName.equals(name) && newType.equals(type)) {
          o = newValue;
        }
        return o;
      }
    };
  }

  /// Returns a new `ParamMap` with all the parameters of this map which have not a name included in the provided
  ///  `names`.
  ///
  /// @param names the names of parameters to be excluded from the new map
  /// @return a new `ParamMap` with all the parameters of this map which have not a name included in the provided
  /// `names`
  default ParamMap without(String... names) {
    ParamMap thisParamMap = this;
    Set<String> newNames = thisParamMap.names();
    List.of(names).forEach(newNames::remove);
    return new ParamMap() {
      @Override
      public int hashCode() {
        return ParamMap.hash(thisParamMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof ParamMap other) {
          return ParamMap.areEquals(this, other);
        }
        return false;
      }

      @Override
      public Set<String> names() {
        return newNames;
      }

      @Override
      public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
        if (!newNames.contains(name)) {
          return null;
        }
        return thisParamMap.value(name, type, enumClass);
      }
    };
  }
}
