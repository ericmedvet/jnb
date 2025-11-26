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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CompositeParseException extends ParseException {

  private final Collection<? extends ParseException> exceptions;

  public CompositeParseException(Collection<? extends ParseException> exceptions) {
    super(collapse(exceptions));
    this.exceptions = exceptions;
  }

  private static ParseException collapse(Collection<? extends ParseException> exceptions) {
    if (exceptions.isEmpty()) {
      throw new IllegalArgumentException("Empty collection of exceptions");
    }
    String s = exceptions.iterator().next().getString();
    Path path = exceptions.iterator().next().getPath();
    int maxIndex = exceptions.stream().mapToInt(ParseException::getIndex).max().orElseThrow();
    Set<TokenType> expectedTokens = exceptions.stream()
        .filter(e -> e.getIndex() == maxIndex)
        .flatMap(e -> expectedToken(e).stream())
        .collect(Collectors.toSet());
    long nOfOtherExceptions = exceptions.stream().filter(e -> !(e instanceof WrongTokenException)).count();
    // TODO if nOfOtherExceptions>0, return a composite exception summarizing everything
    if (!expectedTokens.isEmpty()) {
      return new WrongTokenException(maxIndex, s, path, expectedTokens.stream().toList());
    }
    return exceptions.stream()
        .filter(e -> !(e instanceof WrongTokenException))
        .findFirst()
        .orElseThrow();
  }

  private static Set<TokenType> expectedToken(ParseException ex) {
    return switch (ex) {
      case WrongTokenException wte -> new HashSet<>(wte.getExpectedTokenTypes());
      case CompositeParseException cpe -> cpe.exceptions.stream()
          .flatMap(iex -> expectedToken(iex).stream())
          .collect(Collectors.toSet());
      default -> Set.of();
    };
  }
}