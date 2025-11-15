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

import java.util.Map;
import java.util.WeakHashMap;

@FunctionalInterface
public interface Builder<T> {
  default T build(ParamMap map, NamedBuilder<?> namedBuilder) throws BuilderException {
    return build(map, namedBuilder, 0);
  }

  T build(ParamMap map, NamedBuilder<?> namedBuilder, int index) throws BuilderException;

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
