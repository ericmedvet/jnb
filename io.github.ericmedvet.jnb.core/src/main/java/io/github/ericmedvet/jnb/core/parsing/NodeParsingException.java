package io.github.ericmedvet.jnb.core.parsing;

import java.util.List;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public class NodeParsingException extends ParsingException {
  private final Class<? extends Node> nodeClass;

  public NodeParsingException(
      List<ParsingException> relatedExceptions,
      String string,
      int index,
      Class<? extends Node> parentNode,
      int parentNodeIndex,
      Class<? extends Node> nodeClass
  ) {
    super(relatedExceptions, string, index, parentNode, parentNodeIndex);
    this.nodeClass = nodeClass;
  }

  public Class<? extends Node> getNodeClass() {
    return nodeClass;
  }
}
