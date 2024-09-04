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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ParamMap {

  enum Type {
    INT("i"),
    DOUBLE("d"),
    STRING("s"),
    BOOLEAN("b"),
    ENUM("e"),
    NAMED_PARAM_MAP("npm"),
    INTS("i[]"),
    DOUBLES("d[]"),
    STRINGS("s[]"),
    BOOLEANS("b[]"),
    ENUMS("e[]"),
    NAMED_PARAM_MAPS("npm[]");
    private final String rendered;

    Type(String rendered) {
      this.rendered = rendered;
    }

    public String rendered() {
      return rendered;
    }
  }

  Set<String> names();

  <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass);

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
        pm.names().stream().map(n -> Map.entry(n, pm.value(n))).toArray(Object[]::new));
  }

  default ParamMap and(ParamMap other) {
    ParamMap thisParamMap = this;
    return new ParamMap() {
      @Override
      public int hashCode() {
        return ParamMap.hash(thisParamMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof ParamMap otherMap) {
          return ParamMap.areEquals(this, otherMap);
        }
        return false;
      }

      @Override
      public Set<String> names() {
        return Stream.concat(thisParamMap.names().stream(), other.names().stream())
            .collect(Collectors.toSet());
      }

      @Override
      public <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass) {
        Object o = thisParamMap.value(n, type, enumClass);
        if (o == null) {
          o = other.value(n, type, enumClass);
        }
        return o;
      }
    };
  }

  default Object value(String n, Type type) {
    if (type.equals(Type.ENUM) || type.equals(Type.ENUMS)) {
      throw new IllegalArgumentException("Cannot obtain enum(s) type for \"%s\" without enum class".formatted(n));
    }
    return value(n, type, null);
  }

  default Object value(String n) {
    return Arrays.stream(Type.values())
        .filter(t -> !(t.equals(Type.ENUM) || t.equals(Type.ENUMS)))
        .map(t -> value(n, t))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  default ParamMap with(String name, Type valueType, Object value) {
    ParamMap thisParamMap = this;
    return new ParamMap() {
      @Override
      public int hashCode() {
        return ParamMap.hash(thisParamMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof ParamMap other) {
          return ParamMap.areEquals(this, other);
        }
        return false;
      }

      @Override
      public Set<String> names() {
        return Stream.concat(thisParamMap.names().stream(), Stream.of(name))
            .collect(Collectors.toSet());
      }

      @Override
      public <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass) {
        Object o = thisParamMap.value(n, type, enumClass);
        if (o == null && name.equals(n) && valueType.equals(type)) {
          o = value;
        }
        return o;
      }
    };
  }

  default ParamMap without(String... names) {
    ParamMap thisParamMap = this;
    Set<String> newNames = thisParamMap.names();
    List.of(names).forEach(newNames::remove);
    return new ParamMap() {
      @Override
      public int hashCode() {
        return ParamMap.hash(thisParamMap);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof ParamMap other) {
          return ParamMap.areEquals(this, other);
        }
        return false;
      }

      @Override
      public Set<String> names() {
        return newNames;
      }

      @Override
      public <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass) {
        if (!newNames.contains(n)) {
          return null;
        }
        return thisParamMap.value(n, type, enumClass);
      }
    };
  }
}
