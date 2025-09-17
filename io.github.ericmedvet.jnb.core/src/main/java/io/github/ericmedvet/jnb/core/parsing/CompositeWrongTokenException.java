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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class CompositeWrongTokenException extends WrongTokenException {
  public CompositeWrongTokenException(Collection<WrongTokenException> wtes) {
    super(
        wtes.stream()
            .max(Comparator.comparingInt(WrongTokenException::getIndex))
            .orElseThrow()
            .getIndex(),
        wtes.stream().findFirst().orElseThrow().getString(),
        wtes.stream().findFirst().orElseThrow().getPath(),
        wtes.stream()
            .filter(
                wte -> wte.getIndex() == wtes.stream()
                    .max(Comparator.comparingInt(WrongTokenException::getIndex))
                    .orElseThrow()
                    .getIndex()
            )
            .map(WrongTokenException::getExpectedTokenTypes)
            .flatMap(List::stream)
            .distinct()
            .sorted()
            .toList()
    );
  }
}
