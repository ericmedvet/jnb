package io.github.ericmedvet.jnb.core;
@FunctionalInterface
public interface Builder<T> {
  default T build(ParamMap map, NamedBuilder<?> namedBuilder) throws BuilderException {
    return build(map, namedBuilder, 0);
  }

  T build(ParamMap map, NamedBuilder<?> namedBuilder, int index) throws BuilderException;
}
