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

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// An object that stores elements of type `T` in a modifiable table like structure where cells are
/// indexed through pairs of coordinates `R`,`C`. `R` is the type of row indexes, `C` is the type of
/// column indexes. It provides methods for modifying the content of the cells
/// ([#set(Object, Object, Object)]) or the content and the structure ([#clear()], [#addRow(Object, Map)],
/// [#addColumn(Object, Map)], [#removeRow(Object)], [#removeColumn(Object)]). It also provides methods for obtaining
/// views of (parts) of the table.
///
/// Rows and columns have a well-defined encounter order.
///
/// @param <R> the type of row indexes
/// @param <C> the type of column indexes
/// @param <T> the type of values in the cells
public interface Table<R, C, T> {

  /// A read-only table for which all the methods for modifying the content throw an
  /// [UnsupportedOperationException]. Many methods of [Table] return views which are read-only
  /// tables.
  ///
  /// @param <R> the type of row indexes
  /// @param <C> the type of column indexes
  /// @param <T> the type of values in the cells
  interface Unmodifiable<R, C, T> extends Table<R, C, T> {

    /// Throws an exception, as the table is read-only.
    ///
    /// @param columnIndex the index of the new column
    /// @param values      a map of the values of the new column indexed by row indexes
    /// @throws UnsupportedOperationException always
    @Override
    default void addColumn(C columnIndex, Map<R, T> values) {
      throw new UnsupportedOperationException("This is a read only table");
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param rowIndex the index of the new row
    /// @param values   a map of the values of the new row indexed by column indexes
    /// @throws UnsupportedOperationException always
    @Override
    default void addRow(R rowIndex, Map<C, T> values) {
      throw new UnsupportedOperationException("This is a read only table");
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param columnIndex the index of the column to be removed
    /// @throws UnsupportedOperationException always
    @Override
    default void removeColumn(C columnIndex) {
      throw new UnsupportedOperationException("This is a read only table");
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param rowIndex the index of the row to be removed
    /// @throws UnsupportedOperationException always
    @Override
    default void removeRow(R rowIndex) {
      throw new UnsupportedOperationException("This is a read only table");
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param rowIndex    the row index of cell where to set the value
    /// @param columnIndex the column index of cell where to set the value
    /// @param t           the new value
    /// @throws UnsupportedOperationException always
    @Override
    default void set(R rowIndex, C columnIndex, T t) {
      throw new UnsupportedOperationException("This is a read only table");
    }
  }

  /// Adds or modifies a column to this table. If the table already contains a column of index
  /// `columnIndex`, adds the provided column as the last column in the table. Otherwise, it
  /// modifies the column at `columnIndex` with the provided values. The new column is given as a
  /// `Map<R,T> values` where keys are row indexes. If `values` contains row indexes that are not in
  /// this table, this method adds the corresponding rows to the table, in the order the map
  /// iterator encounters the corresponding keys.
  ///
  /// @param columnIndex the index of the new column
  /// @param values      a map of the values of the new column indexed by row indexes
  void addColumn(C columnIndex, Map<R, T> values);

  /// Adds or modifies a row to this table. If the table already contains a row of index `rowIndex`,
  /// adds the provided row as the last row in the table. Otherwise, it modifies the row at
  /// `rowIndex` with the provided values. The new row is given as a `Map<C,T> values` where keys
  /// are column indexes. If `values` contains column indexes that are not in this table, this
  /// method adds the corresponding columns to the table, in the order the map iterator encounters
  /// the corresponding keys.
  ///
  /// @param rowIndex the index of the new row
  /// @param values   a map of the values of the new column indexed by row indexes
  void addRow(R rowIndex, Map<C, T> values);

  /// Returns the list of column indexes.
  ///
  /// @return the list of column indexes
  List<C> colIndexes();

  /// Returns the value at the cell given by the provided row and column indexes, if any. Returns
  /// `null` if the cell value is actually `null` or if the table does not have a column at
  /// `columnIndex` or a row at `rowIndex`.
  ///
  /// @param rowIndex    the index of the row of the cell
  /// @param columnIndex the index of the column of the cell
  /// @return the value at the cell given by the provided row and column indexes, or `null` if no
  ///  value
  T get(R rowIndex, C columnIndex);

  /// Removes the column at `columnIndex`, if any, from this table.
  /// If the table has no column at `columnIndex`, invoking this method has no effect.
  ///
  /// @param columnIndex the index of the column to remove
  void removeColumn(C columnIndex);

  /// Removes the row at `rowIndex`, if any, from this table.
  /// If the table has no row at `rowIndex`, invoking this method has no effect.
  ///
  /// @param rowIndex the index of the column to remove
  void removeRow(R rowIndex);

  /// Returns the list of row indexes.
  ///
  /// @return the list of row indexes
  List<R> rowIndexes();

  /// Set the value at the cell given by the provided row and column indexes.
  /// If there is not a row at `rowIndex`, adds the row as last row.
  /// If there is not a column at `columnIndex`, adds the column as last column.
  ///
  /// @param rowIndex    the index of the row of the cell
  /// @param columnIndex the index of the column of the cell
  void set(R rowIndex, C columnIndex, T t);

  private static <T> T first(T t1, T t2) {
    return t1;
  }

  private static String justify(String s, int length) {
    if (s.length() > length) {
      return s.substring(0, length);
    }
    StringBuilder sBuilder = new StringBuilder(s);
    while (sBuilder.length() < length) {
      sBuilder.insert(0, " ");
    }
    s = sBuilder.toString();
    return s;
  }

  /// Creates a new unmodifiable table based on the values provided in `map`.
  ///
  /// @param map the values to put in the table, organized in a map of rows, each being a map of column indexes to
  ///                                             values
  /// @param <R> the type of row indexes
  /// @param <C> the type of column indexes
  /// @param <T> the type of values in the cells
  /// @return the new unmodifiable table
  static <R, C, T> Table<R, C, T> of(Map<R, Map<C, T>> map) {
    List<R> rowIndexes = map.keySet().stream().toList();
    List<C> colIndexes = map.values()
        .stream()
        .map(Map::keySet)
        .flatMap(Set::stream)
        .distinct()
        .toList();
    HashMap<R, Map<C, T>> localMap = new HashMap<>(map);
    return new Unmodifiable<>() {

      @Override
      public List<C> colIndexes() {
        return colIndexes;
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        return localMap.getOrDefault(rowIndex, Map.of()).get(columnIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return rowIndexes;
      }
    };
  }

  /// Creates a new unmodifiable table by aggregating this table by rows.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, each ordered partition is fed to `aggregatorFunction` to obtain a single row where values are of type
  ///  `T1`.
  /// Depending on `aggregatorFunction`, the created table may have more or less columns of this table.
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered rows in new rows
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRows(
      Function<Map.Entry<R, Map<C, T>>, K> rowKeyFunction,
      Comparator<R> comparator,
      Function<List<Map.Entry<R, Map<C, T>>>, Map<C, T1>> aggregatorFunction
  ) {
    Map<R, Map<C, T1>> map = rowIndexes().stream()
        .map(ri -> Map.entry(ri, row(ri)))
        .collect(Collectors.groupingBy(rowKeyFunction))
        .values()
        .stream()
        .map(l -> {
          List<Map.Entry<R, Map<C, T>>> list = l.stream()
              .sorted((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey()))
              .toList();
          R ri = list.stream().map(Map.Entry::getKey).min(comparator).orElseThrow();
          Map<C, T1> row = aggregatorFunction.apply(list);
          return Map.entry(ri, row);
        })
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Table::first,
                LinkedHashMap::new
            )
        );
    return Table.of(map);
  }

  /// Creates a new unmodifiable table by aggregating this table by row contents.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, each ordered partition is fed to `aggregatorFunction` to obtain a single row where values are of type
  ///  `T1`.
  /// Depending on `aggregatorFunction`, the created table may have more or less columns of this table.
  /// Internally calls [Table#aggregateRows(Function, Comparator, Function)].
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered rows in new rows
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByContent(
      Function<Map<C, T>, K> rowKeyFunction,
      Comparator<R> comparator,
      Function<List<Map.Entry<R, Map<C, T>>>, Map<C, T1>> aggregatorFunction
  ) {
    return aggregateRows(e -> rowKeyFunction.apply(e.getValue()), comparator, aggregatorFunction);
  }

  /// Creates a new unmodifiable table by aggregating this table by row contents and processing columns independently.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, for each column index of this table, the corresponding cell values of each ordered partition are fed
  /// to `aggregatorFunction` to obtain a single value are of type `T1`.
  /// The created table will have the same number of columns of this table.
  /// Internally calls [Table#aggregateRowsByContentSingle(Function, Comparator, BiFunction)].
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered rows in new rows
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByContentSingle(
      Function<Map<C, T>, K> rowKeyFunction,
      Comparator<R> comparator,
      Function<List<T>, T1> aggregatorFunction
  ) {
    return aggregateRowsByContentSingle(rowKeyFunction, comparator, (c, values) -> aggregatorFunction.apply(values));
  }

  /// Creates a new unmodifiable table by aggregating this table by row contents and processing columns independently.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, for each column index of this table, the corresponding cell values of each ordered partition are fed
  /// to `aggregatorFunction` to obtain a single value are of type `T1`.
  /// The created table will have the same number of columns of this table.
  /// Internally calls [Table#aggregateRowsByContent(Function, Comparator, Function)].
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered rows in new rows
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByContentSingle(
      Function<Map<C, T>, K> rowKeyFunction,
      Comparator<R> comparator,
      BiFunction<C, List<T>, T1> aggregatorFunction
  ) {
    Function<List<Map.Entry<R, Map<C, T>>>, Map<C, T1>> rowAggregator = entries -> entries.getFirst()
        .getValue()
        .keySet()
        .stream()
        .map(
            c -> Map.entry(
                c,
                aggregatorFunction.apply(c, entries.stream().map(e -> e.getValue().get(c)).toList())
            )
        )
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Table::first,
                LinkedHashMap::new
            )
        );
    return aggregateRowsByContent(rowKeyFunction, comparator, rowAggregator);
  }

  /// Creates a new unmodifiable table by aggregating this table by row indexes.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, each ordered partition is fed to `aggregatorFunction` to obtain a single row where values are of type
  ///  `T1`.
  /// Depending on `aggregatorFunction`, the created table may have more or less columns of this table.
  /// Internally calls [Table#aggregateRows(Function, Comparator, Function)].
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered rows in new rows
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByIndex(
      Function<R, K> rowKeyFunction,
      Comparator<R> comparator,
      Function<List<Map.Entry<R, Map<C, T>>>, Map<C, T1>> aggregatorFunction
  ) {
    return aggregateRows(e -> rowKeyFunction.apply(e.getKey()), comparator, aggregatorFunction);
  }

  /// Creates a new unmodifiable table by aggregating this table by row indexes and processing columns independently.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, for each column index of this table, the corresponding cell values of each ordered partition are fed
  /// to `aggregatorFunction` to obtain a single value are of type `T1`.
  /// The created table will have the same number of columns of this table.
  /// Internally calls [Table#aggregateRowsByIndex(Function, Comparator, Function)].
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered cell values in new values
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByIndexSingle(
      Function<R, K> rowKeyFunction,
      Comparator<R> comparator,
      BiFunction<C, List<Map.Entry<R, T>>, T1> aggregatorFunction
  ) {
    Function<List<Map.Entry<R, Map<C, T>>>, Map<C, T1>> rowAggregator = rows -> rows.getFirst()
        .getValue()
        .keySet()
        .stream()
        .map(
            c -> Map.entry(
                c,
                aggregatorFunction.apply(
                    c,
                    rows.stream()
                        .map(
                            r -> Map.entry(
                                r.getKey(),
                                r.getValue().get(c)
                            )
                        )
                        .toList()
                )
            )
        )
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Table::first,
                LinkedHashMap::new
            )
        );
    return aggregateRowsByIndex(rowKeyFunction, comparator, rowAggregator);
  }

  /// Creates a new unmodifiable table by aggregating this table by row indexes and processing columns independently.
  /// First, rows are partitioned using `rowKeyFunction`, using the equivalence of `K` as relation.
  /// Then, rows in each partition are sorted using `comparator`.
  /// Finally, for each column index of this table, the corresponding cell values of each ordered partition are fed
  /// to `aggregatorFunction` to obtain a single value are of type `T1`.
  /// The created table will have the same number of columns of this table.
  /// Internally calls [Table#aggregateRowsByIndexSingle(Function, Comparator, Function)].
  ///
  /// @param rowKeyFunction     the function to partition rows
  /// @param comparator         the comparator to sort rows within each partition
  /// @param aggregatorFunction the function to aggregate ordered cell values in new values
  /// @param <T1>               the type of values of cells in the new table
  /// @param <K>                the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByIndexSingle(
      Function<R, K> rowKeyFunction,
      Comparator<R> comparator,
      Function<List<Map.Entry<R, T>>, T1> aggregatorFunction
  ) {
    BiFunction<C, List<Map.Entry<R, T>>, T1> rowAggregator = (c, rows) -> aggregatorFunction.apply(rows);
    return aggregateRowsByIndexSingle(rowKeyFunction, comparator, rowAggregator);
  }

  /// Clear the table by removing all the rows.
  default void clear() {
    rowIndexes().forEach(this::removeRow);
  }

  default Table<R, C, T> colLeftJoin(Table<R, C, T> other) {
    Table<R, C, T> thisTable = this;
    List<C> colIndexes = Stream.of(colIndexes(), other.colIndexes())
        .flatMap(List::stream)
        .distinct()
        .toList();
    return new Unmodifiable<>() {
      @Override
      public List<C> colIndexes() {
        return colIndexes;
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        T thisT = thisTable.get(rowIndex, columnIndex);
        if (thisT != null) {
          return thisT;
        }
        if (!thisTable.rowIndexes().contains(rowIndex)) {
          return null;
        }
        return other.get(rowIndex, columnIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes();
      }
    };
  }

  default Table<R, C, T> colSlide(int n, Function<List<T>, T> aggregator) {
    Table<R, C, T> table = new HashMapTable<>();
    rowIndexes().forEach(ri -> IntStream.range(n, colIndexes().size()).forEach(i -> {
      List<T> ts = IntStream.range(i - n, i)
          .mapToObj(j -> get(ri, colIndexes().get(j)))
          .toList();
      table.set(ri, colIndexes().get(i - 1), aggregator.apply(ts));
    }));
    return table;
  }

  default Map<R, T> column(C columnIndex) {
    if (!colIndexes().contains(columnIndex)) {
      return Map.of();
    }
    return rowIndexes().stream()
        .filter(ri -> get(ri, columnIndex) != null)
        .collect(
            Collectors.toMap(
                ri -> ri,
                ri -> get(ri, columnIndex),
                Table::first,
                LinkedHashMap::new
            )
        );
  }

  default List<T> columnValues(C columnIndex) {
    Map<R, T> column = column(columnIndex);
    return rowIndexes().stream().map(column::get).toList();
  }

  default List<Map<R, T>> columns() {
    return colIndexes().stream().map(this::column).toList();
  }

  default <C1, T1> Table<R, C1, T1> expandColumn(C columnIndex, BiFunction<R, T, Map<C1, T1>> f) {
    return of(
        rowIndexes().stream()
            .map(ri -> Map.entry(ri, f.apply(ri, get(ri, columnIndex))))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  default Table<R, C, T> expandRowIndex(C c, Function<R, ? extends T> f) {
    return expandRowIndex(r -> Map.ofEntries(Map.entry(c, f.apply(r))));
  }

  default Table<R, C, T> expandRowIndex(Function<R, Map<C, T>> f) {
    return of(
        rowIndexes().stream()
            .map(
                ri -> Map.entry(
                    ri,
                    Stream.of(f.apply(ri).entrySet(), row(ri).entrySet())
                        .flatMap(Set::stream)
                        .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Table::first,
                                LinkedHashMap::new
                            )
                        )
                )
            )
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  default Table<R, C, T> filter(Predicate<Map.Entry<R, Map<C, T>>> predicate) {
    return Table.of(
        rowIndexes().stream()
            .map(ri -> Map.entry(ri, row(ri)))
            .filter(predicate)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
    );
  }

  default Table<R, C, T> filterByRowValue(Predicate<Map<C, T>> predicate) {
    return filter(e -> predicate.test(e.getValue()));
  }

  default Table<R, C, T> filterByRowValue(C colIndex, Predicate<T> predicate) {
    return filterByRowValue(r -> predicate.test(r.get(colIndex)));
  }

  default T get(int x, int y) {
    R ri = rowIndexes().get(y);
    C ci = colIndexes().get(x);
    if (ri == null || ci == null) {
      throw new IndexOutOfBoundsException(
          String.format(
              "Invalid %d,%d coords in a %d,%d table",
              x,
              y,
              colIndexes().size(),
              rowIndexes().size()
          )
      );
    }
    return get(ri, ci);
  }

  default <T1> Table<R, C, T1> map(TriFunction<R, C, T, T1> mapper) {
    Table<R, C, T> thisTable = this;
    return new Unmodifiable<>() {
      @Override
      public List<C> colIndexes() {
        return thisTable.colIndexes();
      }

      @Override
      public T1 get(R rowIndex, C columnIndex) {
        return mapper.apply(rowIndex, columnIndex, thisTable.get(rowIndex, columnIndex));
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes();
      }
    };
  }

  default <T1> Table<R, C, T1> map(Function<T, T1> mapper) {
    Table<R, C, T> thisTable = this;
    return new Unmodifiable<>() {
      @Override
      public List<C> colIndexes() {
        return thisTable.colIndexes();
      }

      @Override
      public T1 get(R rowIndex, C columnIndex) {
        return mapper.apply(thisTable.get(rowIndex, columnIndex));
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes();
      }
    };
  }

  default int nColumns() {
    return colIndexes().size();
  }

  default int nRows() {
    return rowIndexes().size();
  }

  default String prettyToString(
      Function<R, String> rFormat,
      Function<C, String> cFormat,
      Function<T, String> tFormat
  ) {
    if (nColumns() == 0) {
      return "";
    }
    String colSep = " ";
    Map<C, Integer> widths = colIndexes().stream()
        .collect(
            Collectors.toMap(
                ci -> ci,
                ci -> Math.max(
                    cFormat.apply(ci).length(),
                    rowIndexes().stream()
                        .mapToInt(
                            ri -> tFormat.apply(get(ri, ci)).length()
                        )
                        .max()
                        .orElse(0)
                ),
                Table::first,
                LinkedHashMap::new
            )
        );
    int riWidth = rowIndexes().stream()
        .mapToInt(ri -> rFormat.apply(ri).length())
        .max()
        .orElse(0);
    StringBuilder sb = new StringBuilder();
    // print header
    sb.append(justify("", riWidth));
    sb.append(riWidth > 0 ? colSep : "");
    sb.append(
        colIndexes().stream()
            .map(ci -> justify(cFormat.apply(ci), widths.get(ci)))
            .collect(Collectors.joining(colSep))
    );
    if (nRows() == 0) {
      return sb.toString();
    }
    sb.append("\n");
    // print rows
    sb.append(
        rowIndexes().stream()
            .map(ri -> {
              String s = justify(rFormat.apply(ri), riWidth);
              s = s + (riWidth > 0 ? colSep : "");
              s = s + colIndexes().stream()
                  .map(ci -> justify(tFormat.apply(get(ri, ci)), widths.get(ci)))
                  .collect(Collectors.joining(colSep));
              return s;
            })
            .collect(Collectors.joining("\n"))
    );
    return sb.toString();
  }

  default String prettyToString() {
    return prettyToString("%s"::formatted, "%s"::formatted, "%s"::formatted);
  }

  default <R1> Table<R1, C, T> remapRowIndex(BiFunction<R, Map<C, T>, R1> f) {
    return of(
        rowIndexes().stream()
            .map(ri -> Map.entry(f.apply(ri, row(ri)), row(ri)))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  default <R1> Table<R1, C, T> remapRowIndex(Function<R, R1> f) {
    return remapRowIndex((ri, r) -> f.apply(ri));
  }

  default Map<C, T> row(R rowIndex) {
    return colIndexes().stream()
        .filter(ci -> get(rowIndex, ci) != null)
        .collect(
            Collectors.toMap(ci -> ci, ci -> get(rowIndex, ci), Table::first, LinkedHashMap::new)
        );
  }

  default <T1> Table<R, C, T1> rowSlide(int n, Function<List<T>, T1> aggregator) {
    Table<R, C, T1> table = new HashMapTable<>();
    colIndexes().forEach(ci -> IntStream.range(n, rowIndexes().size()).forEach(i -> {
      List<T> ts = IntStream.range(i - n, i)
          .mapToObj(j -> get(rowIndexes().get(j), ci))
          .toList();
      table.set(rowIndexes().get(i - 1), ci, aggregator.apply(ts));
    }));
    return table;
  }

  default List<T> rowValues(R rowIndex) {
    Map<C, T> row = row(rowIndex);
    return colIndexes().stream().map(row::get).toList();
  }

  default List<Map<C, T>> rows() {
    return rowIndexes().stream().map(this::row).toList();
  }

  default Table<R, C, T> select(Predicate<C> predicate) {
    return of(
        rowIndexes().stream()
            .collect(
                Collectors.toMap(
                    ri -> ri,
                    ri -> row(ri).entrySet()
                        .stream()
                        .filter(e -> predicate.test(e.getKey()))
                        .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Table::first,
                                LinkedHashMap::new
                            )
                        ),
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  default Table<R, C, T> select(List<C> columnIndexes) {
    return select(columnIndexes::contains);
  }

  default void set(int x, int y, T t) {
    R ri = rowIndexes().get(y);
    C ci = colIndexes().get(x);
    if (ri == null || ci == null) {
      throw new IndexOutOfBoundsException(
          String.format(
              "Invalid %d,%d coords in a %d,%d table",
              x,
              y,
              colIndexes().size(),
              rowIndexes().size()
          )
      );
    }
    set(ri, ci, t);
  }

  default Table<R, C, T> sorted(Comparator<R> comparator) {
    Table<R, C, T> thisTable = this;
    return new Table<>() {
      @Override
      public void addColumn(C columnIndex, Map<R, T> values) {
        thisTable.addColumn(columnIndex, values);
      }

      @Override
      public void addRow(R rowIndex, Map<C, T> values) {
        thisTable.addRow(rowIndex, values);
      }

      @Override
      public List<C> colIndexes() {
        return thisTable.colIndexes();
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        return thisTable.get(rowIndex, columnIndex);
      }

      @Override
      public void removeColumn(C columnIndex) {
        thisTable.removeColumn(columnIndex);
      }

      @Override
      public void removeRow(R rowIndex) {
        thisTable.removeRow(rowIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes().stream().sorted(comparator).toList();
      }

      @Override
      public void set(R rowIndex, C columnIndex, T t) {
        thisTable.set(rowIndex, columnIndex, t);
      }
    };
  }

  default Table<R, C, T> sorted(C c, Comparator<T> comparator) {
    Table<R, C, T> thisTable = this;
    return new Table<>() {
      @Override
      public void addColumn(C columnIndex, Map<R, T> values) {
        thisTable.addColumn(columnIndex, values);
      }

      @Override
      public void addRow(R rowIndex, Map<C, T> values) {
        thisTable.addRow(rowIndex, values);
      }

      @Override
      public List<C> colIndexes() {
        return thisTable.colIndexes();
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        return thisTable.get(rowIndex, columnIndex);
      }

      @Override
      public void removeColumn(C columnIndex) {
        thisTable.removeColumn(columnIndex);
      }

      @Override
      public void removeRow(R rowIndex) {
        thisTable.removeRow(rowIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes()
            .stream()
            .sorted((ri1, ri2) -> comparator.compare(get(ri1, c), get(ri2, c)))
            .toList();
      }

      @Override
      public void set(R rowIndex, C columnIndex, T t) {
        thisTable.set(rowIndex, columnIndex, t);
      }
    };
  }

  default List<T> values() {
    return rowIndexes().stream()
        .flatMap(ri -> colIndexes().stream().map(ci -> get(ri, ci)))
        .toList();
  }

  default <R1, C1> Table<R1, C1, T> wider(Function<R, R1> rowKey, BiFunction<R, C, C1> spreader) {
    return of(
        rowIndexes().stream()
            .collect(Collectors.groupingBy(rowKey))
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue()
                        .stream()
                        .map(
                            ri -> colIndexes().stream()
                                .map(ci -> Map.entry(spreader.apply(ri, ci), get(ri, ci)))
                                .toList()
                        )
                        .flatMap(List::stream)
                        .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Table::first,
                                LinkedHashMap::new
                            )
                        ),
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  default <R1, C1> Table<R1, C1, T> wider(
      Function<R, R1> rowKey,
      C colIndex,
      Function<R, C1> spreader
  ) {
    return of(
        rowIndexes().stream()
            .collect(Collectors.groupingBy(rowKey))
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue()
                        .stream()
                        .collect(
                            Collectors.toMap(
                                spreader,
                                r -> get(r, colIndex),
                                Table::first,
                                LinkedHashMap::new
                            )
                        ),
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  default Table<R, C, T> with(C newColumnIndex, T newT) {
    List<C> newColIndexes = Stream.of(colIndexes(), List.of(newColumnIndex))
        .flatMap(List::stream)
        .toList();
    Table<R, C, T> thisTable = this;
    return new Unmodifiable<>() {

      @Override
      public List<C> colIndexes() {
        return newColIndexes;
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        if (columnIndex.equals(newColumnIndex)) {
          return newT;
        }
        return thisTable.get(rowIndex, columnIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes();
      }
    };
  }
}
