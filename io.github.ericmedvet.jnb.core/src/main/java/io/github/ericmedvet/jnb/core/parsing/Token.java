package io.github.ericmedvet.jnb.core.parsing;

public record Token(int start, int end) {
  public String trimmedContent(String s) {
    return s.substring(start, end).trim();
  }

  public String trimmedUnquotedContent(String s) {
    return trimmedContent(s).replaceAll("\"", "");
  }

  public int length() {
    return end - start;
  }
}
