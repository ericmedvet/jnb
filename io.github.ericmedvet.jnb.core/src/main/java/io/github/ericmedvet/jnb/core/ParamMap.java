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
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;

public interface ParamMap {

  enum Type {
    INT("i"), DOUBLE("d"), STRING("s"), BOOLEAN("b"), ENUM("e"), NAMED_PARAM_MAP("npm"), INTS("i[]"), DOUBLES(
        "d[]"
    ), STRINGS("s[]"), BOOLEANS("b[]"), ENUMS("e[]"), NAMED_PARAM_MAPS("npm[]");

    private final String rendered;

    Type(String rendered) {
      this.rendered = rendered;
    }

    public String rendered() {
      return rendered;
    }
  }

  Set<String> names();

  SequencedSet<Type> types(String name);

  ParamMap parent();

  void propagateParent(ParamMap paramMap);

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

  default ParamMap and(ParamMap other) {
    Map<String, Object> values = new HashMap<>();
    other.names().forEach(n -> values.put(n, other.value(n)));
    names().forEach(n -> values.put(n, value(n)));
    return new MapNamedParamMap("", values);
  }

  default ParamMap andOverwrite(ParamMap other) {
    Map<String, Object> values = new HashMap<>();
    names().forEach(n -> values.put(n, value(n)));
    other.names().forEach(n -> values.put(n, other.value(n)));
    return new MapNamedParamMap("", values);
  }

  default Object value(String n, Type type) {
    if (type.equals(Type.ENUM) || type.equals(Type.ENUMS)) {
      throw new IllegalArgumentException("Cannot obtain enum(s) type for \"%s\" without enum class".formatted(n));
    }
    return value(n, type, null);
  }

  default Object value(String name) {
    return value(name, types(name).getFirst());
  }

  default ParamMap with(String name, Object value) {
    return andOverwrite(new MapNamedParamMap("", Map.of(name, value)));
  }

  default ParamMap without(String... names) {
    Map<String, Object> values = new HashMap<>();
    names().forEach(n -> values.put(n, value(n)));
    for (String name : names) {
      values.remove(name);
    }
    return new MapNamedParamMap("", values);
  }
}
