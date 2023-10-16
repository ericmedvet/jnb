package io.github.ericmedvet.jnb.core;

import java.util.List;
public interface DocumentedBuilder<T> extends Builder<T> {
  record ParamInfo(
      ParamMap.Type type,
      Class<?> enumClass,
      String name,
      Object defaultValue,
      Param.Injection injection,
      java.lang.reflect.Type javaType) {
    @Override
    public String toString() {
      return String.format(
          "%s = %s%s",
          injection.equals(Param.Injection.NONE) ? name : injection.toString().toLowerCase(),
          type.rendered(),
          defaultValue == null ? "" : ("{" + defaultValue + "}"));
    }
  }

  java.lang.reflect.Type builtType();

  String name();

  List<ParamInfo> params();
}
