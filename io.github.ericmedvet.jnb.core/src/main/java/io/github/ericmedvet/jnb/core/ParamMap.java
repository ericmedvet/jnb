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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ParamMap {

  Set<String> names();

  <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass);

  default Object value(String n) {
    return Arrays.stream(Type.values())
        .filter(t -> !(t.equals(Type.ENUM) || t.equals(Type.ENUMS)))
        .map(t -> value(n, t))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  default Object value(String n, Type type) {
    if (type.equals(Type.ENUM) || type.equals(Type.ENUMS)) {
      throw new IllegalArgumentException("Cannot obtain enum(s) type for \"%s\" without enum class".formatted(n));
    }
    return value(n, type, null);
  }

  default ParamMap and(ParamMap other) {
    ParamMap thisParamMap = this;
    return new ParamMap() {
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
}
