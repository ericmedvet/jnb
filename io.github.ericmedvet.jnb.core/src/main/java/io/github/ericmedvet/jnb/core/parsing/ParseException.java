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

import java.nio.file.Path;
import java.util.Objects;

public class ParseException extends Exception {

  protected static final int CONTEXT_SIZE = 10;
  private final int index;
  private final String string;
  private final Path path;
  private final String rawMessage;

  public ParseException(String rawMessage, Throwable cause, int index, String string, Path path) {
    super(cause);
    this.rawMessage = rawMessage;
    this.index = index;
    this.string = string;
    this.path = path;
  }

  protected static String linearize(String string, int start, int end) {
    return string.substring(Math.max(0, start), Math.min(end, string.length()))
        .replaceAll(StringParser.LINE_TERMINATOR_REGEX, "↲")
        .replaceAll("\\s\\s+", "␣");
  }

  public int getIndex() {
    return index;
  }

  public String getString() {
    return string;
  }

  public Path getPath() {
    return path;
  }

  public String getRawMessage() {
    return rawMessage;
  }

  @Override
  public String getMessage() {
    return "%s @%s %sin `%s`".formatted(
        getRawMessage(),
        StringPosition.from(getString(), getIndex()),
        Objects.nonNull(getPath()) ? "of %s ".formatted(getPath()) : "",
        linearize(getString(), getIndex() - CONTEXT_SIZE, getIndex() + CONTEXT_SIZE)
    );
  }
}