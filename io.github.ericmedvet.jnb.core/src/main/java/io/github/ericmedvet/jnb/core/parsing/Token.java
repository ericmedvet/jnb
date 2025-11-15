/*-
 * ========================LICENSE_START=================================
 * jnb-core
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Token(int start, int end) {
  public String trimmedContent(String s) {
    return s.substring(start, end)
        .replaceAll("\\A" + StringParser.VOID_REGEX, "")
        .replaceAll(StringParser.VOID_REGEX + "\\z", "");
  }

  public String trimmedUnquotedContent(String s) {
    String innerS = s.substring(start, end).replaceAll("\\A" + StringParser.VOID_REGEX, "");
    Matcher m;
    m = Pattern.compile(TokenType.INTERPOLATED_STRING.getRegex()).matcher(innerS);
    if (m.find()) {
      return innerS.substring(m.start(), m.end()).replace(StringParser.INTERPOLATED_STRING_BOUNDARY, "");
    }
    m = Pattern.compile(TokenType.STRING.getRegex()).matcher(innerS);
    if (m.find()) {
      return innerS.substring(m.start(), m.end()).replace(StringParser.QUOTED_STRING_BOUNDARY, "");
    }
    return "";
  }

  public int length() {
    return end - start;
  }
}
