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

import java.util.HashMap;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;

/// An object mapping parameter names (strings) to parameter (typed) values. Values are type by
/// means of [Type], whose constants are associated with Java types. Parameter values can be
/// retrieved by name (with [#value(String)]) or by name and [Type] (with [#value(String, Type)]):
/// with the latter, the actual runtime Java type of the returned value is the one associated with
/// the provided `Type`. Typically, instances of this interface may be obtained by parsing a string,
/// through the [io.github.ericmedvet.jnb.core.parsing.StringParser#parse(String)] map, or by
/// obtaining modified views of an existing `ParamMap`, e.g., through [#with(String, Object)]
/// and [#without(String...)] methods.
///
/// This interface also provides additional static methods to produce human-friendly representations
/// of [ParamMap] objects.
public interface ParamMap {

  /// The type of values that can be stored in a `ParamMap`. Besides the `enum` name, each `Type`
  /// has a short string representation obtainable with [#rendered()], useful for documenting and
  /// debugging builders in a `NamedBuilder`. Each constant is associated with a Java type, which is
  /// the actual one returned (if possible) by [#value(String, Type)].
  enum Type {
    /// Natural numbers, associated with `Integer`; the type is rendered as `i`.
    INT("i"),
    /// Real numbers, associated with `Double`; the type is rendered as `d`.
    DOUBLE("d"),
    /// Strings, associated with `String`; the type is rendered as `s`.
    STRING("s"),
    /// Boolean values, associated with `Boolean`; the type is rendered as `b`.
    BOOLEAN("b"),
    /// Enumerations, associated with an `enum`; the type is rendered as `e`. Values of this type are
    /// constants of a specific `enum` and have to be retrieved through [#value(String, Type,
    /// Class)].
    ENUM("e"),
    /// Named maps of named parameters, associated with [NamedParamMap]; the type is rendered as
    /// `npm`.
    NAMED_PARAM_MAP("npm"),
    /// Lists of natural numbers, associated with `List<Integer>`; the type is rendered as `i[]`.
    INTS("i[]"),
    /// Lists of real numbers, associated with `List<Double>`; the type is rendered as `d[]`.
    DOUBLES("d[]"),
    /// Lists of strings, associated with `List<String>`; the type is rendered as `s[]`.
    STRINGS("s[]"),
    /// Lists of Boolean values, associated with `List<Boolean>`; the type is rendered as `b[]`.
    BOOLEANS("b[]"),
    /// Lists of enumerations, associated with `List<E>`, where `E` is a specific `enum`; the type
    /// is rendered as `e[]`.
    ///
    /// @see Type#ENUM
    ENUMS("e[]"),
    /// Lists of named maps of named parameters, associated with `List<NamedParamMap>`; the type is
    /// rendered as `npm[]`.
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

    @Override
    public String toString() {
      return rendered;
    }
  }

  /// Returns the set of all the names (in no specified order) of the parameters stored in this
  /// map.
  ///
  /// @return the names of the parameters of this map
  Set<String> names();

  // TODO write doc
  SequencedSet<Type> types(String name);

  // TODO write doc
  ParamMap parent();

  // TODO write doc
  // TODO move to MapNamedParamMap
  void propagateParent(ParamMap paramMap);

  /// Returns the value of the parameter stored in this map with the provided `name` and `type`, if
  /// any. The actual runtime Java type of the returned value is the one specified by `type`. If
  /// `type` is [Type#ENUM] or [Type#ENUMS], `enumClass` has to be specified, otherwise `enumClass`
  /// is ignored.
  ///
  /// If the map contains a parameter with the provided `name` but with a different type than
  /// `type`, this method will return `null`.
  ///
  /// @param name      the name of the parameter to get the value of
  /// @param type      the type of the parameter
  /// @param enumClass the (optional) class of the enumerated param value
  /// @param <E>       the type of the (optional) enumeration
  /// @return the value of the parameter with name `name` and type `type`, or `null` otherwise
  <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass);

  /// Returns a new `ParamMap` with all the parameters of this map and those of the provided `other`
  /// map. Hence, the set of names returned by `names()` is the union of the two sets. When
  /// retrieving a value, this map has the precedence over the `other` map.
  ///
  /// @param other the other `ParamMap` to get the values from
  /// @return a new `ParamMap` with all the parameters of this and the `other` maps
  default ParamMap and(ParamMap other) {
    Map<String, Object> values = new HashMap<>();
    other.names().forEach(n -> values.put(n, other.value(n)));
    names().forEach(n -> values.put(n, value(n)));
    return new MapNamedParamMap("", values, parent());
  }

  /// Returns a new `ParamMap` with all the parameters of this map and those of the provided `other`
  /// map. Hence, the set of names returned by `names()` is the union of the two sets. When
  /// retrieving a value, the `other` map has the precedence over this map.
  ///
  /// @param other the other `ParamMap` to get the values from
  /// @return a new `ParamMap` with all the parameters of this and the `other` maps
  default ParamMap andOverwrite(ParamMap other) {
    Map<String, Object> values = new HashMap<>();
    names().forEach(n -> values.put(n, value(n)));
    other.names().forEach(n -> values.put(n, other.value(n)));
    return new MapNamedParamMap("", values);
  }

  /// Returns the value of the parameter stored in this map with the provided `name` and `type`, if
  /// any. Works for all types different from [Type#ENUM] and [Type#ENUMS]. Internally calls
  /// [#value(String, Type, Class)].
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

  /// Returns the value of the parameter stored in this map with the provided `name`, if any. If
  /// more than one types are compatible with the parameter value with this type (e.g., a natural
  /// number called `n` being compatible with both [Type#INT] and [Type#DOUBLE]), then the type
  /// which comes first in the order given by [#types(String)] is chosen. Internally calls
  /// [#value(String, Type)].
  ///
  /// @param name the name of the parameter to get the value of
  /// @return the value of the parameter with name `name`, or `null` otherwise
  default Object value(String name) {
    if (types(name).isEmpty()) {
      return null;
    }
    return value(name, types(name).getFirst());
  }

  /// Returns a new `ParamMap` with all the parameters of this map and another parameter as
  /// provided. If this map already contains a value for the provided name, the new one is returned by the build map.
  ///
  /// @param newName  the name of the new parameter
  /// @param newValue the value of the new parameter
  /// @return a new `ParamMap` with all the parameters of this map and the new parameter
  default ParamMap with(String newName, Object newValue) {
    return andOverwrite(new MapNamedParamMap("", Map.of(newName, newValue)));
  }

  /// Returns a new `ParamMap` with all the parameters of this map which have not a name included in
  /// the provided `names`.
  ///
  /// @param names the names of parameters to be excluded from the new map
  /// @return a new `ParamMap` with all the parameters of this map which have not a name included in
  ///  the provided `names`
  default ParamMap without(String... names) {
    Map<String, Object> values = new HashMap<>();
    names().forEach(n -> values.put(n, value(n)));
    for (String name : names) {
      values.remove(name);
    }
    return new MapNamedParamMap("", values, parent());
  }
}