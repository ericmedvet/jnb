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

import java.util.Objects;
import java.util.Set;

/// A [ParamMap] with a name.
public interface NamedParamMap extends ParamMap {

  /// Returns the name of this named map.
  ///
  /// @return the name of this named map
  String mapName();

  /// Creates a named map from an existing map.
  /// The returned map is a view of the provided `paramMap`.
  ///
  /// @param name the name of the named map
  /// @param paramMap the map the returned named map will be a view of
  /// @return the new named map
  static NamedParamMap from(String name, ParamMap paramMap) {
    return new NamedParamMap() {
      @Override
      public int hashCode() {
        return Objects.hash(name, paramMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof NamedParamMap other) {
          if (!Objects.equals(name, other.mapName())) {
            return false;
          }
          return Objects.equals(paramMap, other);
        }
        return false;
      }

      @Override
      public String toString() {
        return ParamMap.prettyToString(this, Integer.MAX_VALUE);
      }

      @Override
      public String mapName() {
        return name;
      }

      @Override
      public Set<String> names() {
        return paramMap.names();
      }

      @Override
      public <E extends Enum<E>> Object value(String name, Type type, Class<E> enumClass) {
        return paramMap.value(name, type, enumClass);
      }
    };
  }

  @Override
  default ParamMap and(ParamMap other) {
    return from(mapName(), ParamMap.super.and(other));
  }

  @Override
  default NamedParamMap with(String name, Type valueType, Object value) {
    return from(mapName(), ParamMap.super.with(name, valueType, value));
  }

  @Override
  default ParamMap without(String... names) {
    return from(mapName(), ParamMap.super.without(names));
  }
}
