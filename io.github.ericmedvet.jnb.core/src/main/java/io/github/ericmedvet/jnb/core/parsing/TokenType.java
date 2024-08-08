/*-
 * ========================LICENSE_START=================================
 * jnb-core
 * %%
 * Copyright (C) 2023 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jnb.core.parsing;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TokenType {
  NUM("(-?[0-9]+(\\.[0-9]+)?)|(-?Infinity)", "0.0"),
  I_NUM("[0-9]+?", "0"),
  STRING("(" + StringParser.PLAIN_STRING_REGEX + ")|(" + StringParser.QUOTED_STRING_REGEX + ")", "a"),
  CONST_NAME(
      "(" + Pattern.quote(StringParser.CONST_NAME_PREFIX) + StringParser.PLAIN_STRING_REGEX + ")",
      StringParser.CONST_NAME_PREFIX + "a"),
  NAME("[A-Za-z][" + NamedBuilder.NAME_SEPARATOR + "A-Za-z0-9_]*", "a.a"),
  OPEN_CONTENT("\\(", "("),
  CLOSED_CONTENT("\\)", ")"),
  ASSIGN_SEPARATOR("=", "="),
  LIST_SEPARATOR(";", ";"),
  INTERVAL_SEPARATOR(":", ":"),
  OPEN_LIST("\\[", "["),
  CLOSED_LIST("\\]", "]"),
  LIST_JOIN("\\*", "*"),
  LIST_CONCAT("\\+", "+"),
  END_OF_STRING("\\z", "EOS");

  private final String regex;
  private final String rendered;

  TokenType(String regex, String rendered) {
    this.regex = regex;
    this.rendered = rendered;
  }

  public String getRegex() {
    return regex;
  }

  public Token next(String s, int i) throws WrongTokenException {
    Matcher matcher = Pattern.compile(StringParser.VOID_REGEX + regex + StringParser.VOID_REGEX)
        .matcher(s);
    if (!matcher.find(i) || matcher.start() != i) {
      throw new WrongTokenException(i, s, List.of(this));
    }
    return new Token(matcher.start(), matcher.end());
  }

  public String rendered() {
    return rendered;
  }
}
