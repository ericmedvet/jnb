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

import java.util.Objects;
import java.util.Set;

public interface NamedParamMap extends ParamMap {
  String getName();

  static NamedParamMap from(String name, ParamMap paramMap) {
    return new NamedParamMap() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Set<String> names() {
        return paramMap.names();
      }

      @Override
      public <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass) {
        return paramMap.value(n, type, enumClass);
      }

      @Override
      public String toString() {
        return MapNamedParamMap.prettyToString(this, Integer.MAX_VALUE);
      }

      @Override
      public int hashCode() {
        return Objects.hash(name, paramMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof NamedParamMap other) {
          if (!Objects.equals(name, other.getName())) {
            return false;
          }
          return Objects.equals(paramMap, other);
        }
        return false;
      }
    };
  }

  @Override
  default NamedParamMap with(String name, Type valueType, Object value) {
    return from(getName(), ParamMap.super.with(name, valueType, value));
  }

  @Override
  default ParamMap and(ParamMap other) {
    return from(getName(), ParamMap.super.and(other));
  }

  @Override
  default ParamMap without(String... names) {
    return from(getName(), ParamMap.super.without(names));
  }
}
