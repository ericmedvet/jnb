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

public class UndefinedConstantNameException extends RuntimeException {
  private final String notFoundName;
  private final List<String> knownNames;
  private final StringPosition stringPosition;

  public UndefinedConstantNameException(String notFoundName, List<String> knownNames, StringPosition stringPosition) {
    super("Undefined const name %s @%s, known list is %s".formatted(notFoundName, stringPosition, knownNames));
    this.notFoundName = notFoundName;
    this.knownNames = knownNames;
    this.stringPosition = stringPosition;
  }

  public String getNotFoundName() {
    return notFoundName;
  }

  public List<String> getKnownNames() {
    return knownNames;
  }

  public StringPosition getStringPosition() {
    return stringPosition;
  }
}
