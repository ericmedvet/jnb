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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompositeParseException extends ParseException {

  private final List<ParseException> exceptions;

  public CompositeParseException(Collection<ParseException> exceptions) {
    super(null, null, 0, null, null); // TODO improve this with postponed super() of JDK 25
    this.exceptions = lasts(exceptions);
  }

  private static List<ParseException> lasts(Collection<ParseException> exceptions) {
    if (exceptions.isEmpty()) {
      throw new IllegalArgumentException("Empty collection of exceptions");
    }
    List<ParseException> allExceptions = new ArrayList<>();
    exceptions.forEach(e -> {
      if (e instanceof CompositeParseException cpe) {
        allExceptions.addAll(cpe.exceptions);
      } else {
        allExceptions.add(e);
      }
    }
    );
    String s = allExceptions.getFirst().getString();
    Path path = allExceptions.getFirst().getPath();
    int maxIndex = allExceptions.stream().mapToInt(ParseException::getIndex).max().orElseThrow();
    Set<TokenType> expectedTokens = exceptions.stream()
        .filter(e -> e.getIndex() == maxIndex)
        .flatMap(e -> expectedToken(e).stream())
        .collect(Collectors.toSet());
    List<ParseException> lastExceptions = new ArrayList<>();
    if (!expectedTokens.isEmpty()) {
      lastExceptions.add(
          new WrongTokenException(maxIndex, s, path, expectedTokens.stream().toList())
      );
    }
    allExceptions.stream()
        .filter(e -> e.getIndex() == maxIndex)
        .filter(e -> !(e instanceof WrongTokenException))
        .forEach(lastExceptions::add);
    return lastExceptions;
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

  @Override
  public int getIndex() {
    return exceptions.getFirst().getIndex();
  }

  @Override
  public String getString() {
    return exceptions.getFirst().getString();
  }

  @Override
  public Path getPath() {
    return exceptions.getFirst().getPath();
  }

  @Override
  public String getRawMessage() {
    return exceptions.stream()
        .map(ParseException::getRawMessage)
        .distinct()
        .collect(Collectors.joining(", "));
  }
}