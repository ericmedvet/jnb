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

public class ParseException extends Exception {

  protected static final int CONTEXT_SIZE = 10;
  protected final int index;
  protected final String string;

  public ParseException(String message, Throwable cause, int index, String string) {
    super(
        "%s @%s in `%s`"
            .formatted(
                message,
                StringPosition.from(string, index),
                linearize(string, index - CONTEXT_SIZE, index + CONTEXT_SIZE)
            ),
        cause
    );
    this.index = index;
    this.string = string;
  }

  protected static String linearize(String string, int start, int end) {
    return string.substring(Math.max(0, start), Math.min(end, string.length()))
        .replaceAll(StringParser.LINE_TERMINATOR_REGEX, "↲")
        .replaceAll("\\s\\s+", "␣");
  }
}
