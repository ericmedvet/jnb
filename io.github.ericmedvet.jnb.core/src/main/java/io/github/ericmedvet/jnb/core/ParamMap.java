/*
 * Copyright 2022 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jnb.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

  /*
    Boolean b(String n);

    List<Boolean> bs(String n);

    Double d(String n);

    List<Double> ds(String n);

    Integer i(String n);

    List<Integer> is(String n);


    NamedParamMap npm(String n);

    List<NamedParamMap> npms(String n);

    String s(String n);

    List<String> ss(String n);

    default boolean b(String n, boolean defaultValue) {
      return b(n) == null ? defaultValue : b(n);
    }

    default List<Boolean> bs(String n, List<Boolean> defaultValue) {
      return bs(n) == null ? defaultValue : bs(n);
    }

    default double d(String n, double defaultValue) {
      return d(n) == null ? defaultValue : d(n);
    }

    default List<Double> ds(String n, List<Double> defaultValue) {
      return ds(n) == null ? defaultValue : ds(n);
    }

    default <E extends Enum<E>> E e(String n, Class<E> enumClass) {
      return s(n) == null ? null : Enum.valueOf(enumClass, s(n).toUpperCase());
    }

    default <E extends Enum<E>> E e(String n, Class<E> enumClass, E defaultValue) {
      return e(n, enumClass) == null ? defaultValue : e(n, defaultValue.getDeclaringClass());
    }

    default <E extends Enum<E>> List<E> es(String n, Class<E> enumClass) {
      return ss(n) == null ? null : ss(n).stream().map(s -> Enum.valueOf(enumClass, s.toUpperCase())).toList();
    }

    default <E extends Enum<E>> List<E> es(String n, Class<E> enumClass, List<E> defaultValue) {
      return es(n, enumClass) == null ? defaultValue : es(n, enumClass);
    }

    @SuppressWarnings("unused")
    default String fs(String n, String regex) {
      String s = s(n);
      if (s == null || !s.matches(regex)) {
        return null;
      }
      return s;
    }

    @SuppressWarnings("unused")
    default List<String> fss(String n, String regex) {
      List<String> ss = ss(n);
      if (ss == null || ss.stream().filter(s -> s.matches(regex)).count() != ss.size()) {
        return null;
      }
      return ss;
    }

    default int i(String n, int defaultValue) {
      return i(n) == null ? defaultValue : i(n);
    }

    default List<Integer> is(String n, List<Integer> defaultValue) {
      return is(n) == null ? defaultValue : is(n);
    }

    default NamedParamMap npm(String n, NamedParamMap defaultValue) {
      return npm(n) == null ? defaultValue : npm(n);
    }

    default List<NamedParamMap> npms(String n, List<NamedParamMap> defaultValue) {
      return npms(n) == null ? defaultValue : npms(n);
    }

    default String s(String n, String defaultValue) {
      return s(n) == null ? defaultValue : s(n);
    }

    default List<String> ss(String n, List<String> defaultValue) {
      return ss(n) == null ? defaultValue : ss(n);
    }

    default Object value(String n) {
      if (b(n) != null) {
        return b(n);
      }
      if (i(n) != null) {
        return i(n);
      }
      if (d(n) != null) {
        return d(n);
      }
      if (s(n) != null) {
        return s(n);
      }
      if (npm(n) != null) {
        return npm(n);
      }
      if (bs(n) != null) {
        return bs(n);
      }
      if (is(n) != null) {
        return is(n);
      }
      if (ds(n) != null) {
        return ds(n);
      }
      if (ss(n) != null) {
        return ss(n);
      }
      if (npms(n) != null) {
        return npms(n);
      }
      return null;
    }
  */
  /*default <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass) {
    return switch (type) {
      case INT -> i(n);
      case DOUBLE -> d(n);
      case BOOLEAN -> b(n);
      case STRING -> s(n);
      case ENUM -> e(n, enumClass);
      case NAMED_PARAM_MAP -> npm(n);
      case INTS -> is(n);
      case DOUBLES -> ds(n);
      case BOOLEANS -> bs(n);
      case STRINGS -> ss(n);
      case ENUMS -> es(n, enumClass);
      case NAMED_PARAM_MAPS -> npms(n);
    };
  }*/
  Set<String> names();

  <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass);

  default Object value(String n) {
    return Arrays.stream(Type.values()).map(t -> value(n, t)).filter(Objects::nonNull).findFirst().orElse(null);
  }

  default Object value(String n, Type type) {
    if (type.equals(Type.ENUM) || type.equals(Type.ENUMS)) {
      throw new IllegalArgumentException("Cannot obtain enum(s) type for \"%s\" without enum class".formatted(n));
    }
    return value(n, type, null);
  }

}
