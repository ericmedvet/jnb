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

import java.util.List;
import java.util.stream.Collectors;

public class WrongTokenException extends ParseException {
  private final List<TokenType> expectedTokenTypes;

  public WrongTokenException(int index, String string, List<TokenType> expectedTokenTypes) {
    super(
        "`%s` found instead of %s"
            .formatted(
                linearize(string, index, index + 1),
                expectedTokenTypes.stream()
                    .map(tt -> "`%s`".formatted(tt.rendered()))
                    .collect(Collectors.joining(" or "))),
        null,
        index,
        string);
    this.expectedTokenTypes = expectedTokenTypes;
  }

  public int getIndex() {
    return index;
  }

  public String getString() {
    return string;
  }

  public List<TokenType> getExpectedTokenTypes() {
    return expectedTokenTypes;
  }
}
