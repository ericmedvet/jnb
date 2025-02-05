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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

/// An object that maps bi-dimensional integer coordinates defined in a finite domain to values, as in a *grid* like
/// structure.
/// A coordinate $k$ acts as a key and is defined in $\\{0,\dots,w-1\\} \times \\{0,\dots,h-1\\}$, where the *width*
/// $w$ and the *height* $h$ are expected to be immutable and retrievable through [Grid#w()] and [Grid#h()],
/// respectively.
/// Values mapped to (i.e., stored ) by a grid may be `null` and can be set or retrieved through [Grid#get(Key)] and
/// [Grid#set(Key, Object)].
/// Grid *cells* including coordinates and values are modeled through [Grid.Entry].
/// Cells (entries) can be iterated on as a grid is an `Iterable<Grid.Entry<T>>`.
/// Several read-only views of the grid can be obtained through, e.g., [Grid#rows()], [Grid#columns()].
///
/// @param <T> the type of cell values
public interface Grid<T> extends Iterable<Grid.Entry<T>> {
  /// A char used for `true` or non-empty when producing compact string representations of a grid (of Boolean values).
  char FULL_CELL_B_CHAR = '█';
  /// A char used for `false` or empty when producing compact string representations of a grid (of Boolean values).
  char EMPTY_CELL_B_CHAR = '░';
  /// A char used for empty cells when producing compact string representations of a grid of chars.
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

  /// A coordinate of a grid, defined by two `int`s `x` and `y`.
  ///
  /// @param x the x-coordinate
  /// @param y the y-coordinate
  record Key(int x, int y) implements Serializable {
    /// Returns the string representation of this coordinate as `(x,y)`.
    ///
    /// @return the string representation of this coordinate
    @Override
    public String toString() {
      return "(" + x + ',' + y + ')';
    }

    /// Builds a new coordinate by translated this by the offset 'deltaKey`.
    ///
    /// @param deltaKey the offset coordinate
    /// @return the new coordinate
    public Key translated(Key deltaKey) {
      return translated(deltaKey.x(), deltaKey.y());
    }

    /// Builds a new coordinate by translated this by a `dX`, `dY` offset.
    ///
    /// @param dX the offset on the x-coordinate
    /// @param dY the offset on the y-coordinate
    /// @return the new coordinate
    public Key translated(int dX, int dY) {
      return new Key(x + dX, y + dY);
    }
  }

  /// Returns the value at the specified coordinate `key` in this grid, or null if this map contains no mapping for
  /// the key.
  ///
  /// @param key the coordinate at which the value is looked for
  /// @return the value at the specified coordinate
  /// @throws IllegalArgumentException if the key is not valid according to [Grid#isValid(Key)]
  T get(Key key);

  /// Returns the height of the grid, i.e., the smallest non-valid positive y-coordinate.
  /// A key is not valid if its y-coordinate is negative or greater or equal to this returned value.
  ///
  /// @return the height of the grid
  int h();

  /// Set the value at the specified coordinate `key` in this grid.
  ///
  /// @param key the coordinate at which the value is to be set
  /// @param t   the value to set
  /// @throws IllegalArgumentException if the key is not valid according to [Grid#isValid(Key)]
  void set(Key key, T t);

  /// Returns the width of the grid, i.e., the smallest non-valid positive x-coordinate.
  /// A key is not valid if its x-coordinate is negative or greater or equal to this returned value.
  ///
  /// @return the width of the grid
  int w();

  /// Returns a `Collector` taking a stream of grid entries and returning the result as a grid.
  /// The size of the returned grid is determined by considering the largest values for the entry coordinates in the
  /// stream.
  /// Namely, the width is set to the smallest value such that each x-coordinate is lower than it and the height is
  /// set to the smallest value such that each y-coordinate is lower than it.
  /// The grid is created through [Grid#create(int, int)].
  ///
  /// @param <T> the type of values in the input entries
  /// @return a grid containing the mappings specified by the input entries
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

  /// Creates a new grid with the specified width `w` and height `h` and the specified value `t` at each cell of the
  /// grid.
  /// Internally calls [Grid#create(int, int, Function)].
  ///
  /// @param w   the width of the new grid
  /// @param h   the height of the new grid
  /// @param t   the element to be inserted at each cell of the grid
  /// @param <T> the type of cell values
  /// @return the created grid
  static <T> Grid<T> create(int w, int h, T t) {
    return create(w, h, (x, y) -> t);
  }

  /// Creates a new grid with the specified width `w` and height `h` and by putting values generated with the
  /// provided `fillerFunction`.
  /// Internally first instantiates an [ArrayGrid], then fills it.
  ///
  /// @param w              the width of the new grid
  /// @param h              the height of the new grid
  /// @param fillerFunction a function to obtain values given coordinates
  /// @param <T>            the type of cell values
  /// @return the created grid
  static <T> Grid<T> create(int w, int h, Function<Key, T> fillerFunction) {
    Grid<T> grid = new ArrayGrid<>(w, h);
    grid.keys().forEach(k -> grid.set(k, fillerFunction.apply(k)));
    return grid;
  }

  /// Creates a new grid with the specified width `w` and height `h` and by putting values  taken from the provided
  /// list `ts`.
  /// The list size has to be consistent with the size $w \times h$ of the grid.
  /// Consumes the provided values `ts` filling the grid in columns, i.e., the value put at $x,y$ is the $i$-th one,
  ///  with $i=y + h x$.
  /// Internally calls [Grid#create(int, int, Function)].
  ///
  /// @param w   the width of the new grid
  /// @param h   the height of the new grid
  /// @param ts  the provided values
  /// @param <T> the type of cell values
  /// @return the created grid
  /// @throws IllegalArgumentException if the size of `ts` is not $w \times h$
  static <T> Grid<T> create(int w, int h, List<T> ts) {
    if (ts.size() != w * h) {
      throw new IllegalArgumentException(
          "Wrong list size: %d x %d = %d expected, %d found".formatted(w, h, w * h, ts.size())
      );
    }
    return create(w, h, (x, y) -> ts.get(y + h * x));
  }

  /// Creates a new grid with the specified width `w` and height `h` and by putting values generated with the
  /// provided `fillerFunction`.
  /// Internally calls [Grid#create(int, int, Function)].
  ///
  /// @param w              the width of the new grid
  /// @param h              the height of the new grid
  /// @param fillerFunction a function to obtain values given coordinates
  /// @param <T>            the type of cell values
  /// @return the created grid
  static <T> Grid<T> create(int w, int h, BiFunction<Integer, Integer, T> fillerFunction) {
    return create(w, h, k -> fillerFunction.apply(k.x(), k.y()));
  }

  /// Creates a new grid with the specified width `w` and height `h` and empty cells, i.e., with `null` values.
  ///
  /// @param w   the width of the new grid
  /// @param h   the height of the new grid
  /// @param <T> the type of cell values
  /// @return the created grid
  static <T> Grid<T> create(int w, int h) {
    return create(w, h, (T) null);
  }

  /// Returns a compact string representation of a grid containing Boolean values.
  /// The returned string has $h$ lines of $w$ chars each.
  /// Internally calls [Grid#toString(Grid, Predicate)].
  ///
  /// @param grid the grid to represent as a string
  /// @return the compact string representation of the grid
  static String toString(Grid<Boolean> grid) {
    return toString(grid, (Predicate<Boolean>) b -> b);
  }

  /// Returns a compact string representation of a grid given a predicate used for discriminating true and false cells.
  /// The returned string has $h$ lines of $w$ chars each.
  /// Internally calls [Grid#toString(Grid, Function, String)].
  ///
  /// @param grid the grid to represent as a string
  /// @param p    the predicate for discriminating true and false cells
  /// @param <T>  the type of cell values
  /// @return the compact string representation of the grid
  static <T> String toString(Grid<T> grid, Predicate<T> p) {
    return toString(grid, p, "\n");
  }

  /// Returns a compact string representation of a grid given a predicate used for discriminating true and false
  /// cells and a separator of rows.
  /// The returned string has $h$ chunks of $w$ chars each, associated with rows and separated by `separator`.
  /// Internally calls [Grid#toString(Grid, Function)].
  ///
  /// @param grid      the grid to represent as a string
  /// @param p         the predicate for discriminating true and false cells
  /// @param separator the separator of row chunks
  /// @param <T>       the type of cell values
  /// @return the compact string representation of the grid
  static <T> String toString(Grid<T> grid, Predicate<T> p, String separator) {
    return toString(grid, (Entry<T> e) -> p.test(e.value()) ? FULL_CELL_B_CHAR : EMPTY_CELL_B_CHAR, separator);
  }

  /// Returns a compact string representation of a grid given a function to obtain char representations of cell values.
  /// The returned string has $h$ lines of $w$ chars each.
  /// Internally calls [Grid#toString(Grid, Function, String)].
  ///
  /// @param grid     the grid to represent as a string
  /// @param function the function to obtain char representations of cell values
  /// @param <T>      the type of cell values
  /// @return the compact string representation of the grid
  static <T> String toString(Grid<T> grid, Function<T, Character> function) {
    return toString(grid, (Entry<T> e) -> function.apply(e.value()), "\n");
  }

  /// Returns a compact string representation of a grid given a function to obtain char representations of cell
  /// values and a separator of rows.
  /// The returned string has $h$ chunks of $w$ chars each, associated with rows and separated by `separator`.
  ///
  /// @param grid      the grid to represent as a string
  /// @param function  the function to obtain char representations of cell values
  /// @param separator the separator of row chunks
  /// @param <T>       the type of cell values
  /// @return the compact string representation of the grid
  static <T> String toString(Grid<T> grid, Function<Entry<T>, Character> function, String separator) {
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

  /// Returns an immutable view of this grid by columns.
  /// In the outer `List` each element is a column; in the inner `List`, each element is a cell value.
  ///
  /// @return the immutable view by columns of this grid
  default List<List<T>> columns() {
    return IntStream.range(0, w())
        .mapToObj(x -> IntStream.range(0, h()).mapToObj(y -> get(x, y)).toList())
        .toList();
  }

  /// Creates a new grid with the same size and the same content (i.e., cell values) of this grid.
  /// Internally calls [Grid#map(Function)] with the identity.
  ///
  /// @return a copy of this grid
  default Grid<T> copy() {
    return map(Function.identity());
  }

  /// Returns an immutable view of this grid entries.
  /// The returned list of entries is ordered as dictated by [Grid#keys()].
  ///
  /// @return the immutable view by entries of this grid
  default List<Entry<T>> entries() {
    return keys().stream().map(k -> new Entry<>(k, get(k))).toList();
  }

  /// Returns the value at the specified coordinate `x`,`y` in this grid, or null if this map contains no mapping for
  /// the coordinate `x`,`y`.
  ///
  /// @param x the x-coordinate at which the value is looked for
  /// @param y the y-coordinate at which the value is looked for
  /// @return the value at the specified coordinate
  /// @throws IllegalArgumentException if the key is not valid according to [Grid#isValid(Key)]
  default T get(int x, int y) {
    return get(new Key(x, y));
  }

  /// Checks if the provided coordinate `key` is valid based on this grid width and height.
  /// A `key` is not valid if `key.x` is negative, or `key.y` is negative, or `key.x`$\ge$`w`, or `key.y`$\ge$`h`.
  ///
  /// @param key the coordinate to test for validity
  /// @return true if the coordinate is valid, false otherwise
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  default boolean isValid(Key key) {
    return key.x() >= 0 && key.x() < w() && key.y() >= 0 && key.y() < h();
  }

  /// Returns an iterator over the entries of the grid.
  /// The iterator follows the order dictated by [Grid#keys()].
  ///
  /// @return an iterator over the entries of the grid
  default Iterator<Entry<T>> iterator() {
    return entries().iterator();
  }

  /// Returns a list containing all the valid keys of the grid, ordered first by columns, then by rows.
  /// If the grid content is:
  /// ```
  /// a,b,c
  /// d,e,f
  ///```
  /// then the list contains `(0,0),(0,1),(1,0),(1,1),(2,0),(2,1)`.
  ///
  /// @return a list containing the keys of the grid
  default List<Key> keys() {
    return IntStream.range(0, w())
        .mapToObj(
            x -> IntStream
                .range(0, copy().h())
                .mapToObj(y -> new Key(x, y))
        )
        .flatMap(Function.identity())
        .toList();
  }

  /// Creates a new grid of the same size of this grid with values obtained by applying the provided `function` to
  /// this grid values.
  ///
  /// @param function the function to map this grid values to the new grid values
  /// @param <S>      the type of cell values for the new grid
  /// @return the created grid
  default <S> Grid<S> map(Function<T, S> function) {
    return map((k, t) -> function.apply(t));
  }

  /// Creates a new grid of the same size of this grid with values obtained by applying the provided `function` to
  /// this grid coordinates and values.
  ///
  /// @param function the function to map this grid coordinates and values to the new grid values
  /// @param <S>      the type of cell values for the new grid
  /// @return the created grid
  default <S> Grid<S> map(BiFunction<Key, T, S> function) {
    return entries().stream()
        .map(e -> new Entry<>(e.key(), function.apply(e.key(), e.value())))
        .collect(collector());
  }

  /// Returns an immutable view of this grid by rows.
  /// In the outer `List` each element is a row; in the inner `List`, each element is a cell value.
  ///
  /// @return the immutable view by rows of this grid
  default List<List<T>> rows() {
    return IntStream.range(0, h())
        .mapToObj(y -> IntStream.range(0, w()).mapToObj(x -> get(x, y)).toList())
        .toList();
  }

  /// Set the value at the specified coordinate `x`,`y` in this grid.
  ///
  /// @param x the x-coordinate at which the value is to be set
  /// @param y the y-coordinate at which the value is to be set
  /// @param t the value to set
  /// @throws IllegalArgumentException if the key is not valid according to [Grid#isValid(Key)]
  default void set(int x, int y, T t) {
    set(new Key(x, y), t);
  }

  /// Returns a sequential `Stream` of this grid entries.
  ///
  /// @return a sequential stream of the entries of this grid
  default Stream<Entry<T>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /// Returns a view of this grid values in the form of a bi-dimensional array of Booleans based on the provided
  /// predicate `p` applied to values.
  /// The `[x][y]`-th element of the returned array is true if the predicate holds for the grid value at `x`,`y`, and
  ///  false otherwise.
  ///
  /// @param p the predicate for discriminating true and false cells
  /// @return a bi-dimensional array of Booleans
  default boolean[][] toArray(Predicate<T> p) {
    boolean[][] b = new boolean[w()][h()];
    for (Entry<T> entry : this) {
      b[entry.key().x()][entry.key().y()] = p.test(entry.value);
    }
    return b;
  }

  /// Returns an immutable view of this grid values.
  /// The returned list of entries is ordered as dictated by [Grid#keys()].
  ///
  /// @return the immutable view by values of this grid
  default List<T> values() {
    return keys().stream().map(this::get).toList();
  }
}
