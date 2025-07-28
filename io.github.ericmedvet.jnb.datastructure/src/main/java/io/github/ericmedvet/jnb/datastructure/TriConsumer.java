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

/// An operation that accepts three input arguments and returns no result.
/// Unlike most other functional interfaces, `TriConsumer` is expected to operate via side-effects.
///
/// @param <I1> the type of the first input argument
/// @param <I2> the type of the second input argument
/// @param <I3> the type of the third input argument
@FunctionalInterface
public interface TriConsumer<I1, I2, I3> {
  /// Performs this operation on the given arguments.
  ///
  /// @param i1 the first argument
  /// @param i2 the second argument
  /// @param i3 the third argument
  void accept(I1 i1, I2 i2, I3 i3);
}
