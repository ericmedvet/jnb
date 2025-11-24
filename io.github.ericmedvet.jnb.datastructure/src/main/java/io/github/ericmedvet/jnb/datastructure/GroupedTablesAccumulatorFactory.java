/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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

import io.github.ericmedvet.jnb.datastructure.Table.Series;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GroupedTablesAccumulatorFactory<E, V, R, K> implements AccumulatorFactory<E, Map<List<R>, Table<Integer, String, V>>, K> {

  protected final Map<List<R>, Table<Integer, String, V>> data;
  private final List<Function<? super K, ? extends R>> kFunctions;
  private final List<Function<? super E, ? extends V>> eFunctions;

  public GroupedTablesAccumulatorFactory(
      List<Function<? super K, ? extends R>> kFunctions,
      List<Function<? super E, ? extends V>> eFunctions
  ) {
    this.kFunctions = kFunctions;
    this.eFunctions = eFunctions;
    data = new LinkedHashMap<>();
  }

  @Override
  public Accumulator<E, Map<List<R>, Table<Integer, String, V>>> build(K k) {
    List<R> rs = kFunctions.stream().map(nf -> (R) nf.apply(k)).toList();
    Table<Integer, String, V> table;
    synchronized (data) {
      table = data.getOrDefault(rs, new HashMapTable<>());
      data.putIfAbsent(rs, table);
    }
    return new Accumulator<>() {
      @Override
      public Map<List<R>, Table<Integer, String, V>> get() {
        return data;
      }

      @Override
      public void listen(E e) {
        synchronized (data) {
          table.addRow(
              new Series<>(
                  table.nOfRows(),
                  eFunctions.stream().collect(Utils.toSequencedMap(NamedFunction::name, f -> f.apply(e)))
              )
          );
        }
      }
    };
  }
}