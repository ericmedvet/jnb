package io.github.ericmedvet.jnb.core.parsing;

import io.github.ericmedvet.jnb.core.NamedParamMap;

public class StringParser {

  private StringParser() {
  }

  public static NamedParamMap parse(String s) {
    MyParser myParser = new MyParser(s);
    return myParser.parse();
  }

}
