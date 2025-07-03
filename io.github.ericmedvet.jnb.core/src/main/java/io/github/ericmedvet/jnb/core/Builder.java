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

import java.util.Map;
import java.util.WeakHashMap;

/// An object capable of building objects of type `T` from a `ParamMap`.
///
/// @param <T> the type of the object to build
@FunctionalInterface
public interface Builder<T> {
  /// Builds an object of type `T` from a `ParamMap` and a `NamedBuilder`.
  /// This method calls the overloaded `build` method with an index of 0.
  ///
  /// @param map          the `ParamMap` containing the parameters for building the object
  /// @param namedBuilder the `NamedBuilder` that can be used to recursively build values in the `ParamMap`
  /// @return the built object
  /// @throws BuilderException if an error occurs during the building process
  default T build(ParamMap map, NamedBuilder<?> namedBuilder) throws BuilderException {
    return build(map, namedBuilder, 0);
  }

  /// Builds an object of type `T` from a `ParamMap`, a `NamedBuilder`, and an index.
  /// The `ParamMap` is expected to contain the information needed to build the object, as name-value pairs.
  /// The `NamedBuilder` is used to recursively build objects when the values in the `ParamMap` are of a type
  /// different from [io.github.ericmedvet.jnb.core.ParamMap.Type#NAMED_PARAM_MAP] and from
  /// [io.github.ericmedvet.jnb.core.ParamMap.Type#NAMED_PARAM_MAPS].
  /// The `index` is available when building sequences and is expected to be set accordingly by the caller.
  ///
  /// @param map          the `ParamMap` containing the parameters for building the object
  /// @param namedBuilder the `NamedBuilder` that can be used to recursively build values in the `ParamMap`
  /// @param index        the index of the object to build (different from 0 if the object is a part of sequence)
  /// @return the built object
  /// @throws BuilderException if an error occurs during the building process
  T build(ParamMap map, NamedBuilder<?> namedBuilder, int index) throws BuilderException;

  /// Returns a cached version of this `Builder`.
  /// The cached builder stores previously built objects in a cache,
  /// so that if the same `ParamMap` is provided again, the cached object is returned
  /// instead of building a new one.
  /// Internally, the cache is a `WeakHashMap`; the index is ignored when checking for the presence of a map in the cache.
  ///
  /// @return the cached version of this builder
  default Builder<T> cached() {
    Map<ParamMap, T> cache = new WeakHashMap<>();
    Builder<T> thisBuilder = this;
    return (map, namedBuilder, index) -> {
      synchronized (cache) {
        T t = cache.get(map);
        if (t == null) {
          t = thisBuilder.build(map, namedBuilder, index);
          cache.put(map, t);
        }
        return t;
      }
    };
  }
}
