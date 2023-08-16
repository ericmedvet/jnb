package io.github.ericmedvet.jnb.core.parsing;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public interface Node {
  @FunctionalInterface
  interface Parser<N extends Node> {
    N parse(String s, int index, Class<? extends Node> parentNodeClass, int parentNodeIndex);
  }

  Token token();
}
