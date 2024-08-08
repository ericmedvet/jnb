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

public class UndefinedConstantNameException extends ParseException {
  private final String notFoundName;
  private final List<String> knownNames;

  public UndefinedConstantNameException(int index, String string, String notFoundName, List<String> knownNames) {
    super("Const name %s not in known list %s".formatted(notFoundName, knownNames), null, index, string);
    this.notFoundName = notFoundName;
    this.knownNames = knownNames;
  }

  public String getNotFoundName() {
    return notFoundName;
  }

  public List<String> getKnownNames() {
    return knownNames;
  }
}
