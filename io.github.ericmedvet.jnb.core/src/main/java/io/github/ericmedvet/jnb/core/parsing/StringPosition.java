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

public record StringPosition(int lineIndex, int charIndex, int lineStartCharIndex) {
  @Override
  public String toString() {
    return "%d:%d".formatted(lineIndex + 1, charIndex + 1);
  }

  public static StringPosition from(String s, int index) {
    Pattern pattern = Pattern.compile(StringParser.LINE_TERMINATOR_REGEX);
    Matcher matcher = pattern.matcher(s);
    int l = 0;
    int currentLineEndIndex;
    int lastLineStartIndex = 0;
    while (matcher.find(lastLineStartIndex)) {
      currentLineEndIndex = matcher.end();
      if (index < currentLineEndIndex) {
        return new StringPosition(l, index - lastLineStartIndex, lastLineStartIndex);
      }
      l = l + 1;
      lastLineStartIndex = matcher.end();
    }
    return new StringPosition(l, index - lastLineStartIndex, lastLineStartIndex);
  }
}
