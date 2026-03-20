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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import org.jspecify.annotations.Nullable;

/// A table that internally stores its values in an [HashMap].
///
/// @param <R> the type of row indexes
/// @param <C> the type of column indexes
/// @param <T> the type of values in the cells
public class HashMapTable<R, C, T extends @Nullable Object> implements Table<R, C, T>, Copyable<HashMapTable<R, C, T>> {

  private final Map<Key<R, C>, T> map;
  private final SequencedSet<R> rowIndexes;
  private final SequencedSet<C> colIndexes;

  /// Constructs an empty `HashMapTable`.
  public HashMapTable() {
    this.map = new HashMap<>();
    rowIndexes = new LinkedHashSet<>();
    colIndexes = new LinkedHashSet<>();
  }

  @Override
  public void addColumn(Series<C, R, T> column) {
    colIndexes.add(column.primaryIndex());
    rowIndexes.addAll(column.values().keySet());
    column.values()
        .forEach((rowIndex, value) -> map.put(new Key<>(rowIndex, column.primaryIndex()), value));
  }

  @Override
  public void addRow(Series<R, C, T> row) {
    rowIndexes.add(row.primaryIndex());
    colIndexes.addAll(row.values().keySet());
    row.values()
        .forEach((colIndex, value) -> map.put(new Key<>(row.primaryIndex(), colIndex), value));
  }

  @Override
  public SequencedSet<C> colIndexes() {
    return colIndexes;
  }

  /// Returns a shallow copy of this table. Elements and indexes in the new table are the same of the ones in
  /// this table.
  ///
  /// @return a shallow copy of this grid
  @Override
  public HashMapTable<R, C, T> copyOf() {
    HashMapTable<R, C, T> copy = new HashMapTable<>();
    rowIndexes().forEach(ri -> copy.addRow(new Series<>(ri, row(ri))));
    return copy;
  }

  @Override
  public T get(R rowIndex, C colIndex) {
    return map.get(new Key<>(rowIndex, colIndex));
  }

  @Override
  public void removeColumn(C colIndex) {
    colIndexes.remove(colIndex);
    List<Key<R, C>> toRemoveKeys = map.keySet().stream().filter(k -> k.c.equals(colIndex)).toList();
    toRemoveKeys.forEach(map.keySet()::remove);
  }

  @Override
  public void removeRow(R rowIndex) {
    rowIndexes.remove(rowIndex);
    List<Key<R, C>> toRemoveKeys = map.keySet().stream().filter(k -> k.r.equals(rowIndex)).toList();
    toRemoveKeys.forEach(map.keySet()::remove);
  }

  @Override
  public SequencedSet<R> rowIndexes() {
    return rowIndexes;
  }

  @Override
  public void set(R rowIndex, C colIndex, T t) {
    rowIndexes.add(rowIndex);
    colIndexes.add(colIndex);
    map.put(new Key<>(rowIndex, colIndex), t);
  }

  /// Returns a string representation of this table, which shows only the number of rows and columns.
  /// For a more informative, yet human-friendly string representation, see
  ///  [Table#prettyToString(Function , Function, Function)].
  ///
  /// @return a string representation of this table
  @Override
  public String toString() {
    return "Table[%dx%d]".formatted(nOfRows(), nOfColumns());
  }

  private record Key<R, C>(R r, C c) {

  }
}