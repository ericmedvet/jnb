package io.github.ericmedvet.jnb.core.parsing;

import io.github.ericmedvet.jnb.core.NamedBuilder;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public enum TokenType {
  NUM("\\s*-?[0-9]+(\\.[0-9]+)?\\s*", ""),
  I_NUM("\\s*[0-9]+?\\s*", ""),
  STRING("\\s*([A-Za-z][A-Za-z0-9_]*)|(\"[^\"]+\")\\s*", ""),
  NAME("\\s*[A-Za-z][" + NamedBuilder.NAME_SEPARATOR + "A-Za-z0-9_]*\\s*", ""),
  OPEN_CONTENT("\\s*\\(\\s*", "("),
  CLOSED_CONTENT("\\s*\\)\\s*", ")"),
  ASSIGN_SEPARATOR("\\s*=\\s*", "="),
  LIST_SEPARATOR("\\s*;\\s*", ";"),
  INTERVAL_SEPARATOR("\\s*:\\s*", ":"),
  OPEN_LIST("\\s*\\[\\s*", "["),
  CLOSED_LIST("\\s*\\]\\s*", "]"),
  LIST_JOIN("\\s*\\*\\s*", "*"),
  LIST_CONCAT("\\s*\\+\\s*", "+");
  private final String regex;
  private final String rendered;

  TokenType(String regex, String rendered) {
    this.regex = regex;
    this.rendered = rendered;
  }

  public String getRegex() {
    return regex;
  }

  Optional<Token> next(String s, int i) {
    Matcher matcher = Pattern.compile(regex).matcher(s);
    if (!matcher.find(i)) {
      return Optional.empty();
    }
    if (matcher.start() != i) {
      return Optional.empty();
    }
    return Optional.of(new Token(matcher.start(), matcher.end()));
  }

  public String rendered() {
    return rendered;
  }
}
