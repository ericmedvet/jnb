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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class TwoDGridArchive<V> extends GridArchive<V> implements TwoDKeyArchive<V> {

  private final Map<Polygon, Point> cells;

  public TwoDGridArchive(Axis xAxis, Axis yAxis) {
    super(List.of(xAxis, yAxis));
    List<DoubleRange> xSubRanges = xAxis.range().split(xAxis.nOfBins());
    List<DoubleRange> ySubRanges = yAxis.range().split(yAxis.nOfBins());
    cells = xSubRanges.stream()
        .flatMap(
            xr -> ySubRanges.stream()
                .map(
                    yr -> Map.entry(
                        new Polygon(
                            List.of(
                                new Point(xr.min(), yr.min()),
                                new Point(xr.max(), yr.min()),
                                new Point(xr.max(), yr.max()),
                                new Point(xr.min(), yr.max())
                            )
                        ),
                        new Point(xr.center(), yr.center())
                    )
                )
        )
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public Optional<V> put(double x, double y, V value) {
    return put(List.of(x, y), value);
  }

  @Override
  public Optional<V> get(double x, double y) {
    return get(List.of(x, y));
  }

  @Override
  public Map<Polygon, Optional<V>> localizedValues() {
    return cells.entrySet()
        .stream()
        .map(
            e -> Map.entry(
                e.getKey(),
                get(e.getValue().x(), e.getValue().y())
            )
        )
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
