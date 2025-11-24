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

/// An object which has a modifiable parameter of type `P`.
///
/// @param <T> the type of the object being parametrized
/// @param <P> the type of the parameter
public interface Parametrized<T extends Parametrized<T, P>, P> {
  /// Gets the parameter value.
  ///
  /// @return the parameter value
  P getParams();

  /// Sets the parameter to a new value.
  ///
  /// @param param the new parameter value
  void setParams(P param);

  /// Returns this object with the parameter reset to the provided value.
  /// Internally calls [Parametrized#setParams(Object)] and then returns `this`.
  /// Useful for adopting the chain invocation.
  ///
  /// @param param the new parameter value
  /// @return this object after having set the parameter value`
  default T withParams(P param) {
    setParams(param);
    //noinspection unchecked
    return (T) this;
  }
}
