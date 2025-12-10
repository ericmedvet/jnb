/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
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
package io.github.ericmedvet.jnb.datastructure;

/// Provides a basic implementation of the `Composed` interface. It holds an inner object of type
/// `C` and provides a way to access it.
///
/// @param <C> the type of the inner object
public abstract class AbstractComposed<C> implements Composed<C> {

  private final C inner;

  /// Constructs an abstract composed with the provided `inner` object.
  ///
  /// @param inner the inner object
  public AbstractComposed(C inner) {
    this.inner = inner;
  }

  @Override
  public C inner() {
    return inner;
  }
}
