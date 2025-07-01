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
import io.github.ericmedvet.jnb.core.ParamMap.Type;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// A `Builder` which provides additional information about its capability. Namely, it provides its
/// name, the type of the built objects, the param it takes, and the method it has been built from.
///
/// @param <T> the type of the object to build
public interface DocumentedBuilder<T> extends Builder<T> {

  /// Returns the Java type of objects being built by this builder.
  ///
  /// @return the Java type of objects being built
  java.lang.reflect.Type builtType();

  /// Returns the name of this builder.
  ///
  /// @return the name of this builder
  String name();

  /// Returns the list of params of this builder.
  ///
  /// @return the params of this builder
  List<ParamInfo> params();

  /// Returns the executable this builder has been built from.
  ///
  /// @return the executable this builder has been built from
  Executable origin();

  private Object aliasedParamDefaultValue(ParamMap.Type type, Class<?> clazz, String value) {
    if (type.equals(ParamMap.Type.INT)) {
      return Integer.parseInt(value);
    }
    if (type.equals(ParamMap.Type.DOUBLE)) {
      return Double.parseDouble(value);
    }
    if (type.equals(ParamMap.Type.BOOLEAN)) {
      return Boolean.parseBoolean(value);
    }
    if (type.equals(ParamMap.Type.STRING)) {
      return value;
    }
    if (type.equals(ParamMap.Type.ENUM)) {
      //noinspection rawtypes,unchecked
      return Enum.valueOf((Class) clazz, value.toUpperCase());
    }
    if (type.equals(ParamMap.Type.NAMED_PARAM_MAP)) {
      return value;
    }
    if (type.equals(ParamMap.Type.INTS)) {
      return value;
    }
    if (type.equals(ParamMap.Type.DOUBLES)) {
      return value;
    }
    if (type.equals(ParamMap.Type.BOOLEANS)) {
      return value;
    }
    if (type.equals(ParamMap.Type.STRINGS)) {
      return value;
    }
    if (type.equals(ParamMap.Type.ENUMS)) {
      return value;
    }
    if (type.equals(ParamMap.Type.NAMED_PARAM_MAPS)) {
      return value;
    }
    return null;
  }

  /// Returns a new `DocumentedBuilder` that is an alias of this builder.
  /// The new builder has name dictated by `alias.name()` and uses this builder as dictated by `alias.value()`.
  /// Optionally, can define new params that are passed to this builder through `alias.passThroughParams()`.
  ///
  /// @param alias the alias to apply
  /// @return a new `DocumentedBuilder`
  default DocumentedBuilder<T> alias(Alias alias) {
    NamedParamMap preMap = AutoBuiltDocumentedBuilder.fromAlias(alias, null);
    List<ParamInfo> newParams = Stream.concat(
        params().stream()
            .map(
                pi -> new ParamInfo(
                    pi.type,
                    pi.enumClass,
                    pi.name,
                    preMap.names().contains(pi.name) ? preMap.value(pi.name) : pi.defaultValue,
                    pi.interpolationString,
                    pi.injection,
                    pi.javaType
                )
            ),
        Arrays.stream(alias.passThroughParams())
            .map(
                p -> new ParamInfo(
                    p.type(),
                    null,
                    p.name(),
                    aliasedParamDefaultValue(p.type(), p.enumClass(), p.value()),
                    null,
                    Injection.NONE,
                    String.class
                )
            )
    )
        .toList();
    // descriptions for passThroughParams and defaulted params using do not show that
    // they are linked. however, drastic changes are needed in parsing result to fix this
    return new AutoBuiltDocumentedBuilder<>(
        alias.name(),
        builtType(),
        newParams,
        origin(),
        (map, namedBuilder, index) -> build(
            map.and(AutoBuiltDocumentedBuilder.fromAlias(alias, map))
                .without(
                    Arrays.stream(alias.passThroughParams())
                        .map(PassThroughParam::name)
                        .toArray(String[]::new)
                ),
            namedBuilder,
            index
        )
    );
  }

  /// A record storing the information about a param of a `DocumentedBuilder`.
  ///
  /// @param type                the type of the param
  /// @param enumClass           the (optional) `enum` class of the param (if any)
  /// @param name                the name of the param
  /// @param defaultValue        the (optional) default value of the param
  /// @param interpolationString the (optional) interpolation string of the param
  /// @param injection           an enum saying if the param, when building, has to be filled with
  ///                            special values
  /// @param javaType            the Java type of the param
  record ParamInfo(
      ParamMap.Type type,
      Class<?> enumClass,
      String name,
      Object defaultValue,
      String interpolationString,
      Param.Injection injection,
      java.lang.reflect.Type javaType
  ) {

    /// Returns a string representation of this `ParamInfo`, in the form `name = type[defaultValue]`
    /// or `name = type`.
    ///
    /// @return a string representation
    @Override
    public String toString() {
      String defaultValueString = defaultValue != null ? defaultValue.toString() : "";
      if (type.equals(Type.STRING) && interpolationString != null && defaultValueString.isEmpty()) {
        defaultValueString = interpolationString;
      }
      return String.format(
          "%s = %s%s",
          injection.equals(Param.Injection.NONE) ? name : injection.toString().toLowerCase(),
          type.rendered(),
          defaultValueString.isEmpty() ? "" : ("[" + defaultValueString + "]")
      );
    }
  }
}
