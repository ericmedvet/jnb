/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
 * %%
 * Copyright (C) 2023 - 2026 Eric Medvet
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
/*
 * Copyright 2026 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jnb.datastructure;


/// An object that can produce a copy of itself.
///
/// Implementing this interface indicates that instances of the class support the creation of a
/// duplicate object. Unlike the standard [java.lang.Cloneable] interface, `Copyable` provides a
/// strongly-typed contract and does not rely on checked exceptions.
///
/// ## Shallow vs. Deep Copy:
/// The general contract of this interface does not strictly dictate whether the copy must be
/// shallow or deep. It is highly recommended that implementing classes clearly specify the depth of
/// the copy operation in their own documentation. However, whenever possible, implementations
/// should strive to return a fully independent copy (deep copy) to prevent unintended side
/// effects.
///
/// @param <T> the type of the object being copied, which must be the implementing class itself
public interface Copyable<T extends Copyable<T>> {

  /// Returns a copy of this object.
  ///
  /// The returned object should typically be independent of the instance it was copied from.
  /// Modifying the state of the copy should not affect the original object, and vice versa.
  ///
  /// @return a copy of this object, never `null`
  T copyOf();

}