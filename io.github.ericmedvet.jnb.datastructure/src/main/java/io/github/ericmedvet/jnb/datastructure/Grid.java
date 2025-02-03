/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
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

package io.github.ericmedvet.jnb.datastructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/// An object that maps bi-dimensional integer coordinates defined in a finite domain to values, as in a *grid* like
/// structure.
/// A coordinate $k$ acts as a key and is defined in $\{0,\dots,w-1\} \times \{0,\dots,h-1\}$, where $w$ and $h$ are
/// immutable and retrievable through [Grid#w()] and [Grid#h()], respectively.
/// Values mapped to (i.e., stored ) by a grid may be `null` and can be set or retrieved through [Grid#get(Key)] and
/// [Grid#set(Key, Object)].
/// Grid cells including coordinates and values are modeled through [Grid.Entry].
/// Cells (entries) can be iterated on as a grid is an `Iterable<Grid.Entry<T>>`.
/// Several read-only views of the grid can be obtained through, e.g., [Grid#rows()], [Grid#columns()].
///
/// @param <T> the type of mapped values
public interface Grid<T> extends Iterable<Grid.Entry<T>> {
  char FULL_CELL_B_CHAR = '█';
  char EMPTY_CELL_B_CHAR = '░';
  char EMPTY_CELL_C_CHAR = '.';

  /// A grid entry, i.e., a pair of a coordinate `key` and a value `value` describing a cell of the grid.
  ///
  /// @param key   the coordinate of the entry
  /// @param value the value of the entry
  /// @param <V>   the type of the value
  record Entry<V>(Key key, V value) implements Serializable {
    @Override
    public String toString() {
      return key.toString() + "->" + value;
    }
  }

  /// The coordinate of a grid, defined by two `int`s `x` and `y`.
  ///
  /// @param x the x-coordinate
  /// @param y the y-coordinate
  record Key(int x, int y) implements Serializable {
    /// Builds a new coordinate by translated this by a `dX`, `dY` offset.
    ///
    /// @param dX the offset on the x-coordinate
    /// @param dY the offset on the y-coordinate
    /// @return the new coordinate
    public Key translated(int dX, int dY) {
      return new Key(x + dX, y + dY);
    }

    /// Builds a new coordinate by translated this by the offset 'deltaKey`.
    ///
    /// @param deltaKey the offset coordinate
    /// @return the new coordinate
    public Key translated(Key deltaKey) {
      return translated(deltaKey.x(), deltaKey.y());
    }

    /// Returns the string representation of this coordinate as `(x,y)`.
    ///
    /// @return the string representation of this coordinate
    @Override
    public String toString() {
      return "(" + x + ',' + y + ')';
    }
  }

  T get(Key key);

  int h();

  void set(Key key, T t);

  int w();

  static <T> Collector<Entry<T>, ?, Grid<T>> collector() {
    return Collectors.collectingAndThen(
        Collectors.toList(),
        list -> {
          int maxX = list.stream()
              .map(e -> e.key().x())
              .max(Comparator.comparingInt(v -> v))
              .orElse(0);
          int maxY = list.stream()
              .map(e -> e.key().y())
              .max(Comparator.comparingInt(v -> v))
              .orElse(0);
          Grid<T> grid = create(maxX + 1, maxY + 1);
          list.forEach(e -> grid.set(e.key(), e.value()));
          return grid;
        }
    );
  }

  static <K> Grid<K> create(int w, int h, K k) {
    return create(w, h, (x, y) -> k);
  }

  static <K> Grid<K> create(int w, int h, Function<Key, K> fillerFunction) {
    Grid<K> grid = new ArrayGrid<>(w, h);
    grid.keys().forEach(k -> grid.set(k, fillerFunction.apply(k)));
    return grid;
  }

  static <K> Grid<K> create(int w, int h, List<K> ks) {
    if (ks.size() != w * h) {
      throw new IllegalArgumentException(
          "Wrong list size: %d x %d = %d expected, %d found".formatted(w, h, w * h, ks.size())
      );
    }
    return create(w, h, (x, y) -> ks.get(y + h * x));
  }

  static <K> Grid<K> create(int w, int h, BiFunction<Integer, Integer, K> fillerFunction) {
    return create(w, h, k -> fillerFunction.apply(k.x(), k.y()));
  }

  static <K> Grid<K> create(int w, int h) {
    return create(w, h, (K) null);
  }

  static String toString(Grid<Boolean> grid) {
    return toString(grid, (Predicate<Boolean>) b -> b);
  }

  static <K> String toString(Grid<K> grid, Predicate<K> p) {
    return toString(grid, p, "\n");
  }

  static <K> String toString(Grid<K> grid, Predicate<K> p, String separator) {
    return toString(grid, (Entry<K> e) -> p.test(e.value()) ? FULL_CELL_B_CHAR : EMPTY_CELL_B_CHAR, separator);
  }

  static <K> String toString(Grid<K> grid, Function<K, Character> function) {
    return toString(grid, (Entry<K> e) -> function.apply(e.value()), "\n");
  }

  static <K> String toString(Grid<K> grid, Function<Entry<K>, Character> function, String separator) {
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < grid.h(); y++) {
      for (int x = 0; x < grid.w(); x++) {
        Character c = function.apply(new Entry<>(new Key(x, y), grid.get(x, y)));
        sb.append(c != null ? c : EMPTY_CELL_C_CHAR);
      }
      if (y < grid.h() - 1) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

  default List<List<T>> columns() {
    List<List<T>> columns = new ArrayList<>();
    for (int x = 0; x < w(); x++) {
      List<T> column = new ArrayList<>();
      for (int y = 0; y < h(); y++) {
        column.add(get(x, y));
      }
      columns.add(column);
    }
    return columns;
  }

  default Grid<T> copy() {
    return map(Function.identity());
  }

  default List<Entry<T>> entries() {
    return keys().stream().map(k -> new Entry<>(k, get(k))).toList();
  }

  default T get(int x, int y) {
    return get(new Key(x, y));
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  default boolean isValid(Key key) {
    return key.x() >= 0 && key.x() < w() && key.y() >= 0 && key.y() < h();
  }

  default Iterator<Entry<T>> iterator() {
    return entries().iterator();
  }

  default List<Key> keys() {
    List<Key> keys = new ArrayList<>(w() * h());
    for (int x = 0; x < w(); x++) {
      for (int y = 0; y < h(); y++) {
        keys.add(new Key(x, y));
      }
    }
    return Collections.unmodifiableList(keys);
  }

  default <S> Grid<S> map(Function<T, S> function) {
    return map((k, t) -> function.apply(t));
  }

  default <S> Grid<S> map(BiFunction<Key, T, S> function) {
    return entries().stream()
        .map(e -> new Entry<>(e.key(), function.apply(e.key(), e.value())))
        .collect(collector());
  }

  default List<List<T>> rows() {
    List<List<T>> rows = new ArrayList<>();
    for (int y = 0; y < h(); y++) {
      List<T> row = new ArrayList<>();
      for (int x = 0; x < w(); x++) {
        row.add(get(x, y));
      }
      rows.add(row);
    }
    return rows;
  }

  default void set(int x, int y, T t) {
    set(new Key(x, y), t);
  }

  default Stream<Entry<T>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean[][] toArray(Predicate<T> p) {
    boolean[][] b = new boolean[w()][h()];
    for (Entry<T> entry : this) {
      b[entry.key().x()][entry.key().y()] = p.test(entry.value);
    }
    return b;
  }

  default List<T> values() {
    return keys().stream().map(this::get).toList();
  }
}
