package io.github.ericmedvet.jnb.core.parsing;

import java.util.List;
import java.util.Objects;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public abstract class ParsingException extends RuntimeException {

  private final static int CONTEXT_SIZE = 10;
  private final List<ParsingException> relatedExceptions;
  private final String string;
  private final int index;
  private final Class<? extends Node> parentNode;
  private final int parentNodeIndex;

  public ParsingException(
      List<ParsingException> relatedExceptions,
      String string,
      int index,
      Class<? extends Node> parentNode,
      int parentNodeIndex
  ) {
    this.relatedExceptions = relatedExceptions;
    this.string = string;
    this.index = index;
    this.parentNode = parentNode;
    this.parentNodeIndex = parentNodeIndex;
  }

  @Override
  public String toString() {
    String content;
    if (this instanceof NodeParsingException npe) {
      content = npe.getNodeClass()!=null?npe.getNodeClass().getSimpleName():"null"; // TODO remove null check
    } else if (this instanceof TokenParsingException tpe) {
      content = tpe.getTokenType()!=null?("`" + tpe.getTokenType().rendered() + "`"):"null"; // TODO remove null check
    } else {
      content = "Something valid";
    }
    return "%s not found at %d in `%s`[%d:%d] while parsing %s".formatted(
        content,
        index,
        string.substring(
            Math.max(index - CONTEXT_SIZE, 0),
            Math.min(index + CONTEXT_SIZE, string.length())
        ).replaceAll("\\n", "⏎"),
        Math.max(index - CONTEXT_SIZE, 0),
        Math.min(index + CONTEXT_SIZE, string.length() - 1),
        Objects.isNull(parentNode) ? "the root" : parentNode.getSimpleName()
    );
  }
}
