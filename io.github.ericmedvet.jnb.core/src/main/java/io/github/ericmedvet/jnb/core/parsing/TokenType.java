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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TokenType {
  NUM("\\s*(-?[0-9]+(\\.[0-9]+)?)|(-?Infinity)\\s*", "0.0"),
  I_NUM("\\s*[0-9]+?\\s*", "0"),
  STRING("\\s*([A-Za-z][A-Za-z0-9_]*)|(\"[^\"]*\")\\s*", "a"),
  NAME("\\s*[A-Za-z][" + NamedBuilder.NAME_SEPARATOR + "A-Za-z0-9_]*\\s*", "a.a"),
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
