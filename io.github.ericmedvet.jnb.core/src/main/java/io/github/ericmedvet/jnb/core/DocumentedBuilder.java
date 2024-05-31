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

import io.github.ericmedvet.jnb.core.ParamMap.Type;
import java.lang.reflect.Executable;
import java.util.List;

public interface DocumentedBuilder<T> extends Builder<T> {
  java.lang.reflect.Type builtType();

  String name();

  List<ParamInfo> params();

  Executable origin();

  default DocumentedBuilder<T> alias(NamedParamMap preMap) {
    List<ParamInfo> newParams = params().stream()
        .map(pi -> new ParamInfo(
            pi.type,
            pi.enumClass,
            pi.name,
            preMap.names().contains(pi.name) ? preMap.value(pi.name) : pi.defaultValue,
            pi.interpolationString,
            pi.injection,
            pi.javaType))
        .toList();
    return new AutoBuiltDocumentedBuilder<>(
        preMap.getName(),
        builtType(),
        newParams,
        origin(),
        (map, namedBuilder, index) -> build(map.and(preMap), namedBuilder, index));
  }

  record ParamInfo(
      ParamMap.Type type,
      Class<?> enumClass,
      String name,
      Object defaultValue,
      String interpolationString,
      Param.Injection injection,
      java.lang.reflect.Type javaType) {
    @Override
    public String toString() {
      String defaultValueString = defaultValue != null ? defaultValue.toString() : "";
      if (type.equals(Type.STRING) && interpolationString != null && defaultValueString.isEmpty()) {
        defaultValueString = interpolationString;
      }
      return String.format(
          "%s = %s%s",
          injection.equals(Param.Injection.NONE)
              ? name
              : injection.toString().toLowerCase(),
          type.rendered(),
          defaultValueString.isEmpty() ? "" : ("[" + defaultValueString + "]"));
    }
  }
}
