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

public interface ListNode<N extends Node> extends Node {
  List<N> children();

  static <N extends Node> ListNode<N> from(Token token, List<N> children) {
    return new ListNode<>() {
      @Override
      public List<N> children() {
        return children;
      }

      @Override
      public Token token() {
        return token;
      }
    };
  }
}