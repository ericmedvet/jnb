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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ParseException extends Exception {

  protected static final int CONTEXT_SIZE = 10;
  protected final int index;
  protected final String string;
  protected final Path path;

  public ParseException(String message, Throwable cause, int index, String string, Path path) {
    super(
        "%s @%s %sin `%s`"
            .formatted(
                message,
                StringPosition.from(string, index),
                Objects.nonNull(path) ? "of %s ".formatted(path) : "",
                linearize(string, index - CONTEXT_SIZE, index + CONTEXT_SIZE)
            ),
        cause
    );
    this.index = index;
    this.string = string;
    this.path = path;
  }

  public ParseException(Collection<? extends ParseException> exceptions) {
    this(
        collapse(exceptions).getMessage(),
        collapse(exceptions).getCause(),
        collapse(exceptions).index,
        collapse(exceptions).string,
        collapse(exceptions).path
    );
  }

  private static ParseException collapse(Collection<? extends ParseException> exceptions) {
    if (exceptions.isEmpty()) {
      throw new IllegalArgumentException("Empty collection of exceptions");
    }
    String s = exceptions.iterator().next().getString();
    Path path = exceptions.iterator().next().getPath();
    int maxIndex = exceptions.stream().mapToInt(ParseException::getIndex).max().orElseThrow();
    List<WrongTokenException> wtes = exceptions.stream()
        .filter(e -> e.getIndex() == maxIndex)
        .filter(e -> e instanceof WrongTokenException)
        .map(e -> (WrongTokenException) e)
        .toList();
    if (!wtes.isEmpty()) {
      List<TokenType> expectedTokens = wtes.stream()
          .flatMap(e -> e.getExpectedTokenTypes().stream())
          .distinct()
          .toList();
      return new WrongTokenException(maxIndex, s, path, expectedTokens);
    }
    return exceptions.stream().filter(e -> !(e instanceof WrongTokenException)).findFirst().orElseThrow();
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
}
