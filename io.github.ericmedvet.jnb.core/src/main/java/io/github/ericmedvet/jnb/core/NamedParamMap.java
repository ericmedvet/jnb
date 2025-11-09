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

import java.util.HashMap;
import java.util.Map;

public interface NamedParamMap extends ParamMap {
  static NamedParamMap from(String name, ParamMap paramMap) {
    return new MapNamedParamMap(name, paramMap);
  }

  @Override
  default NamedParamMap and(ParamMap other) {
    Map<String, Object> values = new HashMap<>();
    other.names().forEach(n -> values.put(n, other.value(n)));
    names().forEach(n -> values.put(n, value(n)));
    return new MapNamedParamMap(getName(), values);
  }

  @Override
  default NamedParamMap andOverwrite(ParamMap other) {
    Map<String, Object> values = new HashMap<>();
    names().forEach(n -> values.put(n, value(n)));
    other.names().forEach(n -> values.put(n, other.value(n)));
    return new MapNamedParamMap(getName(), values);
  }

  String getName();

  @Override
  default NamedParamMap with(String name, Object value) {
    return andOverwrite(new MapNamedParamMap("", Map.of(name, value)));
  }

  @Override
  default NamedParamMap without(String... names) {
    Map<String, Object> values = new HashMap<>();
    names().forEach(n -> values.put(n, value(n)));
    for (String name : names) {
      values.remove(name);
    }
    return new MapNamedParamMap(getName(), values);
  }
}
