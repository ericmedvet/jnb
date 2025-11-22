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

import io.github.ericmedvet.jnb.core.Param.Injection;
import io.github.ericmedvet.jnb.core.ParamMap.Type;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public interface DocumentedBuilder<T> extends Builder<T> {

  java.lang.reflect.Type builtType();

  String name();

  List<ParamInfo> params();

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

  record ParamInfo(
      ParamMap.Type type,
      Class<?> enumClass,
      String name,
      Object defaultValue,
      String interpolationString,
      Param.Injection injection,
      java.lang.reflect.Type javaType
  ) {

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
