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
package io.github.ericmedvet.jnb.buildable;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

/** @author "Eric Medvet" on 2024/07/26 for jnb */
public enum MathOp implements ToDoubleFunction<double[]> {
  ADD(vs -> Arrays.stream(vs).sum()), SUBTRACT(vs -> switch (vs.length) {
    case 1 -> -vs[0];
    case 2 -> vs[0] - vs[1];
    default -> throw new IllegalArgumentException("Subtract expects 1 or 2 args, found %d".formatted(vs.length));
  }), MULTIPLY(vs -> Arrays.stream(vs).reduce(1, (p, v) -> p * v)), DIVIDE(vs -> switch (vs.length) {
    case 1 -> 1 / vs[0];
    case 2 -> vs[0] / vs[1];
    default -> throw new IllegalArgumentException("Divide expects 1 or 2 args, found %d".formatted(vs.length));
  }), POWER(vs -> {
    if (vs.length == 2) {
      return Math.pow(vs[0], vs[1]);
    }
    throw new IllegalArgumentException("Power expects 2 args, found %d".formatted(vs.length));
  });

  private final ToDoubleFunction<double[]> f;

  MathOp(ToDoubleFunction<double[]> f) {
    this.f = f;
  }

  @Override
  public double applyAsDouble(double[] vs) {
    return f.applyAsDouble(vs);
  }
}
