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

import java.util.random.RandomGenerator;

/// An object which has a modifiable parameter of type `double[]`, i.e., parametrized with numerical parameters.
///
/// @param <T> the type of the object being parametrized
public interface NumericalParametrized<T extends NumericalParametrized<T>> extends Parametrized<T, double[]> {
  /// Randomizes the numerical parameters of this object.
  /// Changes each numerical parameter, i.e., element of the `double[]` parameter of this object, to a new value
  /// chosen with uniform probability in `range`.
  ///
  /// @param randomGenerator the random generator
  /// @param range           the interval from which to sample new parameter values with uniform probability
  default void randomize(RandomGenerator randomGenerator, DoubleRange range) {
    double[] oldParams = getParams();
    double[] newParams = new double[oldParams.length];
    for (int i = 0; i < newParams.length; i++) {
      newParams[i] = range.denormalize(randomGenerator.nextDouble());
    }
    setParams(newParams);
  }
}
