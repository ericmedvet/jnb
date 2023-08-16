package io.github.ericmedvet.jnb.core.parsing;

import java.util.List;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public class TokenParsingException extends ParsingException {
  private final TokenType tokenType;

  public TokenParsingException(
      List<ParsingException> relatedExceptions,
      String string,
      int index,
      Class<? extends Node> parentNode,
      int parentNodeIndex,
      TokenType tokenType
  ) {
    super(relatedExceptions, string, index, parentNode, parentNodeIndex);
    this.tokenType = tokenType;
  }

  public TokenType getTokenType() {
    return tokenType;
  }
}
