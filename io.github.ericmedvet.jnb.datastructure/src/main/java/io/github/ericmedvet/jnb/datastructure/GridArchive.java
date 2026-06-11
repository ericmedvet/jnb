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

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class GridArchive<V> extends AbstractHasherArchive<List<Double>, List<DoubleRange>, V> implements NumericalKeyArchive<V> {

  public record Axis(DoubleRange range, int nOfBins) {

  }

  private final List<Axis> axes;
  private final List<List<DoubleRange>> cells;
  private final int capacity;

  public GridArchive(List<Axis> axes) {
    this.axes = Collections.synchronizedList(axes);
    capacity = axes.stream().mapToInt(Axis::nOfBins).reduce((n1, n2) -> n1 * n2).orElse(0);
    cells = Collections.synchronizedList(axes.stream().map(a -> a.range.split(a.nOfBins)).toList());
  }

  @Override
  public List<DoubleRange> hash(List<Double> key) {
    if (key.size() != axes.size()) {
      throw new IllegalArgumentException(
          "Wrong key size: %d expected, %d found".formatted(axes.size(), key.size())
      );
    }
    return IntStream.range(0, axes.size())
        .mapToObj(i -> cellOf(key.get(i), axes.get(i), cells.get(i)))
        .toList();
  }

  private static DoubleRange cellOf(double value, Axis axis, List<DoubleRange> subranges) {
    return subranges.get(
        (int) Math.clamp(axis.range.normalize(value) * axis.nOfBins, 0, axis.nOfBins - 1)
    );
  }

  @Override
  public int arity() {
    return axes.size();
  }

  @Override
  public int capacity() {
    return capacity;
  }
}