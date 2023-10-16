package io.github.ericmedvet.jnb.core;

import java.util.Arrays;
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
    return Arrays.stream(Type.values())
        .map(t -> value(n, t))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  default Object value(String n, Type type) {
    if (type.equals(Type.ENUM) || type.equals(Type.ENUMS)) {
      throw new IllegalArgumentException(
          "Cannot obtain enum(s) type for \"%s\" without enum class".formatted(n));
    }
    return value(n, type, null);
  }
}
