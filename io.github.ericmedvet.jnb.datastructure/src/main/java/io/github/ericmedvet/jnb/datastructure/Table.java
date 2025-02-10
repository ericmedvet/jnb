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
/// column indexes.
/// Rows and columns have a well-defined encounter order.
///
/// This interface provides methods for modifying the content of the cells ([#set(Object,
/// Object, Object)]) or the content and the structure ([#clear()], [#addRow(Series)],
/// [#addColumn(Series)], [#removeRow(Object)], [#removeColumn(Object)]). It also provides
/// methods for obtaining views of (parts) of the table.
///
/// For creating an unmodifiable table from data, one can use the `from` methods: [#from(SequencedMap)],
/// [#fromRows(List)], and [#fromColumns(List)].
///
/// @param <R> the type of row indexes
/// @param <C> the type of column indexes
/// @param <T> the type of values in the cells
public interface Table<R, C, T> {

  private static <T> T first(T t1, T t2) {
    return t1;
  }

  /// Creates a new unmodifiable table based on the values provided in `map`, organized in a map of
  /// rows, each being a map of column indexes to values.
  ///
  /// @param map the values to put in the table
  /// @param <R> the type of row indexes
  /// @param <C> the type of column indexes
  /// @param <T> the type of values in the cells
  /// @return the new unmodifiable table
  static <R, C, T> Table<R, C, T> from(SequencedMap<R, ? extends SequencedMap<C, T>> map) {
    SequencedSet<R> rowIndexes = Collections.unmodifiableSequencedSet(
        new LinkedHashSet<>(map.keySet())
    );
    SequencedSet<C> colIndexes = Collections.unmodifiableSequencedSet(
        (SequencedSet<? extends C>) map.values()
            .stream()
            .map(Map::keySet)
            .flatMap(Set::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new))
    );
    HashMap<R, Map<C, T>> localMap = new HashMap<>(map);
    localMap.keySet().retainAll(rowIndexes);
    localMap.values().forEach(row -> row.keySet().retainAll(colIndexes));
    return Unmodifiable.of(
        rowIndexes,
        colIndexes,
        (ri, ci) -> localMap.getOrDefault(ri, Map.of()).get(ci)
    );
  }

  /// Creates a new unmodifiable table based on the values provided in `columns`.
  ///
  /// @param columns the columns containing the values to be put in the table
  /// @param <R>     the type of row indexes
  /// @param <C>     the type of column indexes
  /// @param <T>     the type of values in the cells
  /// @return the new unmodifiable table
  static <R, C, T> Table<R, C, T> fromColumns(List<Series<C, R, T>> columns) {
    SequencedMap<R, SequencedMap<C, T>> map = new LinkedHashMap<>();
    columns.forEach(
        col -> col.values
            .forEach(
                (ri, t) -> map.computeIfAbsent(ri, lri -> new LinkedHashMap<>())
                    .put(col.primaryIndex, t)
            )
    );
    return from(map);
  }

  /// Creates a new unmodifiable table based on the values provided in `rows`.
  ///
  /// @param rows the rows containing the values to be put in the table
  /// @param <R>  the type of row indexes
  /// @param <C>  the type of column indexes
  /// @param <T>  the type of values in the cells
  /// @return the new unmodifiable table
  static <R, C, T> Table<R, C, T> fromRows(List<Series<R, C, T>> rows) {
    SequencedMap<R, SequencedMap<C, T>> map = rows.stream()
        .collect(
            Collectors.toMap(
                r -> r.primaryIndex,
                Series::values,
                Table::first,
                LinkedHashMap::new
            )
        );
    return from(map);
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

  /// Adds or modifies a column into this table. If the table already contains a column with the
  /// index corresponding to the `column` primary index, adds the provided series as the last column
  /// in the table. Otherwise, it modifies the column at `column.primaryIndex()` with the provided
  /// series. If `column` contains secondary (row) indexes that are not in this table, this method
  /// adds the corresponding rows to the table, in the order the map iterator encounters the
  /// corresponding keys.
  ///
  /// @param column the new column
  void addColumn(Series<C, R, T> column);

  /// Adds or modifies a row into this table. If the table already contains a row with the index
  /// corresponding to the `row` primary index, adds the provided series as the last row in the
  /// table. Otherwise, it modifies the column at `row.primaryIndex()` with the provided series. If
  /// `row` contains secondary (column) indexes that are not in this table, this method adds the
  /// corresponding columns to the table, in the order the map iterator encounters the corresponding
  /// keys.
  ///
  /// @param row the new row
  void addRow(Series<R, C, T> row);

  /// Returns a new unmodifiable table by aggregating this table by rows. First, rows are
  /// partitioned using `classifier`, using the equivalence of `K` as relation. Then, rows in each
  /// partition are sorted using `comparator`. Finally, each ordered partition is fed to
  /// `aggregator` to obtain a single row where values are of type `T1` and the row index is the
  /// first one of the partition. Depending on `aggregator`, the created table may have more or less
  /// columns of this table.
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered rows in new rows
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRows(
      Function<Series<R, C, T>, K> classifier,
      Comparator<R> comparator,
      Function<List<Series<R, C, T>>, SequencedMap<C, T1>> aggregator
  ) {
    return Table.fromRows(
        rowIndexes().stream()
            .map(ri -> new Series<>(ri, row(ri)))
            .collect(Collectors.groupingBy(classifier))
            .values()
            .stream()
            .map(l -> {
              List<Series<R, C, T>> rows = l.stream()
                  .sorted((r1, r2) -> comparator.compare(r1.primaryIndex(), r2.primaryIndex()))
                  .toList();
              R ri = rows.stream().map(Series::primaryIndex).min(comparator).orElseThrow();
              SequencedMap<C, T1> row = aggregator.apply(rows);
              return new Series<>(ri, row);
            })
            .toList()
    );
  }

  /// Returns a new unmodifiable table by aggregating this table by row contents. First, rows are
  /// partitioned using `classifier`, using the equivalence of `K` as relation. Then, rows in each
  /// partition are sorted using `comparator`. Finally, each ordered partition is fed to
  /// `aggregator` to obtain a single row where values are of type `T1` and the row index is the
  /// first one of the partition. Depending on `aggregator`, the created table may have more or less
  /// columns of this table. Internally calls [Table#aggregateRows(Function, Comparator,
  /// Function)].
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered rows in new rows
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByContent(
      Function<SequencedMap<C, T>, K> classifier,
      Comparator<R> comparator,
      Function<List<Series<R, C, T>>, SequencedMap<C, T1>> aggregator
  ) {
    return aggregateRows(e -> classifier.apply(e.values()), comparator, aggregator);
  }

  /// Returns a new unmodifiable table by aggregating this table by row contents and processing
  /// columns independently. First, rows are partitioned using `classifier`, using the equivalence
  /// of `K` as relation. Then, rows in each partition are sorted using `comparator`. Finally, for
  /// each column index of this table, the corresponding cell values of each ordered partition are
  /// fed to `aggregator` to obtain a single value are of type `T1` and the row index is the first
  /// one of the partition. The created table will have the same number of columns of this table.
  /// Internally calls [Table#aggregateRowsByContentSingle(Function, Comparator, BiFunction)].
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered rows in new rows
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByContentSingle(
      Function<SequencedMap<C, T>, K> classifier,
      Comparator<R> comparator,
      Function<List<T>, T1> aggregator
  ) {
    return aggregateRowsByContentSingle(
        classifier,
        comparator,
        (c, values) -> aggregator.apply(values)
    );
  }

  /// Returns a new unmodifiable table by aggregating this table by row contents and processing
  /// columns independently. First, rows are partitioned using `classifier`, using the equivalence
  /// of `K` as relation. Then, rows in each partition are sorted using `comparator`. Finally, for
  /// each column index of this table, the corresponding cell values of each ordered partition are
  /// fed to `aggregator` to obtain a single value are of type `T1` and the row index is the first
  /// one of the partition. The created table will have the same number of columns of this table.
  /// Internally calls [Table#aggregateRowsByContent(Function, Comparator, Function)].
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered rows in new rows
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByContentSingle(
      Function<SequencedMap<C, T>, K> classifier,
      Comparator<R> comparator,
      BiFunction<C, List<T>, T1> aggregator
  ) {
    Function<List<Series<R, C, T>>, SequencedMap<C, T1>> rowAggregator = rows -> rows.getFirst().values
        .keySet()
        .stream()
        .map(
            c -> Map.entry(
                c,
                aggregator.apply(c, rows.stream().map(row -> row.values.get(c)).toList())
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
    return aggregateRowsByContent(classifier, comparator, rowAggregator);
  }

  /// Returns a new unmodifiable table by aggregating this table by row indexes. First, rows are
  /// partitioned using `classifier`, using the equivalence of `K` as relation. Then, rows in each
  /// partition are sorted using `comparator`. Finally, each ordered partition is fed to
  /// `aggregator` to obtain a single row where values are of type `T1` and the row index is the
  /// first in the partition. Depending on `aggregator`, the created table may have more or less
  /// columns of this table. Internally calls [Table#aggregateRows(Function, Comparator,
  /// Function)].
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered rows in new rows
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByIndex(
      Function<R, K> classifier,
      Comparator<R> comparator,
      Function<List<Series<R, C, T>>, SequencedMap<C, T1>> aggregator
  ) {
    return aggregateRows(row -> classifier.apply(row.primaryIndex), comparator, aggregator);
  }

  /// Returns a new unmodifiable table by aggregating this table by row indexes and processing
  /// columns independently. First, rows are partitioned using `classifier`, using the equivalence
  /// of `K` as relation. Then, rows in each partition are sorted using `comparator`. Finally, for
  /// each column index of this table, the corresponding cell values of each ordered partition are
  /// fed to `aggregator` to obtain a single value of type `T1` to be put in a row with index as the
  /// first row in the partition. The created table will have the same number of columns of this
  /// table. Internally calls [Table#aggregateRowsByIndex(Function, Comparator, Function)].
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered cell values in new values
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByIndexSingle(
      Function<R, K> classifier,
      Comparator<R> comparator,
      BiFunction<C, List<Map.Entry<R, T>>, T1> aggregator
  ) {
    Function<List<Series<R, C, T>>, SequencedMap<C, T1>> rowAggregator = rows -> rows.getFirst().values
        .keySet()
        .stream()
        .map(
            c -> Map.entry(
                c,
                aggregator.apply(
                    c,
                    rows.stream()
                        .map(
                            row -> Map.entry(
                                row.primaryIndex,
                                row.values.get(c)
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
    return aggregateRowsByIndex(classifier, comparator, rowAggregator);
  }

  /// Returns a new unmodifiable table by aggregating this table by row indexes and processing
  /// columns independently. First, rows are partitioned using `classifier`, using the equivalence
  /// of `K` as relation. Then, rows in each partition are sorted using `comparator`. Finally, for
  /// each column index of this table, the corresponding cell values of each ordered partition are
  /// fed to `aggregator` to obtain a single value of type `T1` to be put in a row with index as the
  /// first row in the partition. The created table will have the same number of columns of this
  /// table. Internally calls [Table#aggregateRowsByIndexSingle(Function, Comparator, Function)].
  ///
  /// @param classifier the function to partition rows
  /// @param comparator the comparator to sort rows within each partition
  /// @param aggregator the function to aggregate ordered cell values in new values
  /// @param <T1>       the type of values of cells in the new table
  /// @param <K>        the type of keys to partition rows
  /// @return the new unmodifiable table
  default <T1, K> Table<R, C, T1> aggregateRowsByIndexSingle(
      Function<R, K> classifier,
      Comparator<R> comparator,
      Function<List<Map.Entry<R, T>>, T1> aggregator
  ) {
    BiFunction<C, List<Map.Entry<R, T>>, T1> rowAggregator = (c, rows) -> aggregator.apply(
        rows
    );
    return aggregateRowsByIndexSingle(classifier, comparator, rowAggregator);
  }

  /// Clear the table by removing all the rows.
  default void clear() {
    rowIndexes().forEach(this::removeRow);
  }

  /// Returns the column indexes.
  ///
  /// @return the column indexes
  SequencedSet<C> colIndexes();

  /// Returns a new unmodifiable table which has the row indexes of this table and the union of the
  /// column indexes of this and the `other` table. In each cell of the new table at row $r$ and
  /// column $c$, the value is the one of this table, if present, or the other table, if present, or
  /// empty. All the new column indexes are added after the ones of this table in the new table.
  ///
  /// @param other the other table to join on the left to this one
  /// @return the new unmodifiable table
  default Unmodifiable<R, C, T> colLeftJoin(Table<R, C, T> other) {
    Table<R, C, T> thisTable = this;
    SequencedSet<C> colIndexes = Stream.of(colIndexes(), other.colIndexes())
        .flatMap(SequencedSet::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return Unmodifiable.of(
        rowIndexes(),
        colIndexes,
        (rowIndex, colIndex) -> {
          T thisT = thisTable.get(rowIndex, colIndex);
          if (thisT != null) {
            return thisT;
          }
          if (!thisTable.rowIndexes().contains(rowIndex)) {
            return null;
          }
          return other.get(rowIndex, colIndex);
        }
    );
  }

  /// Returns a new unmodifiable table built by collapsing values in sliding windows of `n` columns
  /// of this table using the `aggregator` function. The new table has the same row indexes of this
  /// table but `n`-1 columns. The first column index of the new table is the `n`-th of this table.
  ///
  /// @param n          the size of slice, i.e., the number of adjacent columns to collapse
  /// @param aggregator the function for collapsing values in a single value
  /// @return the new unmodifiable table
  default Table<R, C, T> collapseSlidingCols(int n, Function<List<T>, T> aggregator) {
    List<C> cis = colIndexes().stream().toList();
    List<Series<R, C, T>> rows = rowIndexes().stream()
        .map(
            ri -> new Series<>(
                ri,
                IntStream.range(n, cis.size())
                    .boxed()
                    .collect(
                        Collectors.toMap(
                            cis::get,
                            i -> aggregator.apply(
                                IntStream.range(i - n, i).mapToObj(j -> get(ri, cis.get(j))).toList()
                            ),
                            Table::first,
                            LinkedHashMap::new
                        )
                    )
            )
        )
        .toList();
    return Table.fromRows(rows);
  }

  /// Returns a new unmodifiable table built by collapsing values in sliding windows of `n` rows of
  /// this table using the `aggregator` function. The new table has the same column indexes of this
  /// table but `n`-1 rows. The first row index of the new table is the `n`-th of this table.
  ///
  /// @param n          the size of slice, i.e., the number of adjacent rows to collapse
  /// @param aggregator the function for collapsing values in a single value
  /// @return the new unmodifiable table
  default Table<R, C, T> collapseSlidingRows(int n, Function<List<T>, T> aggregator) {
    List<R> ris = rowIndexes().stream().toList();
    List<Series<C, R, T>> columns = colIndexes().stream()
        .map(
            ci -> new Series<>(
                ci,
                IntStream.range(n, ris.size())
                    .boxed()
                    .collect(
                        Collectors.toMap(
                            ris::get,
                            i -> aggregator.apply(
                                IntStream.range(i - n, i).mapToObj(j -> get(ris.get(j), ci)).toList()
                            ),
                            Table::first,
                            LinkedHashMap::new
                        )
                    )
            )
        )
        .toList();
    return Table.fromColumns(columns);
  }

  /// Returns an unmodifiable view of the column at `colIndex` of this table, or an empty map if no
  /// such column exists.
  ///
  /// @param colIndex the index of the column
  /// @return a map with the values in the cells of the column indexed by row indexes
  default SequencedMap<R, T> column(C colIndex) {
    if (!colIndexes().contains(colIndex)) {
      return Collections.unmodifiableSequencedMap(new LinkedHashMap<>());
    }
    return Collections.unmodifiableSequencedMap(
        (SequencedMap<? extends R, ? extends T>) rowIndexes().stream()
            .filter(ri -> get(ri, colIndex) != null)
            .collect(
                Collectors.toMap(
                    ri -> ri,
                    ri -> get(ri, colIndex),
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  /// Returns an unmodifiable view of the values in the column at `colIndex` of this table, or an
  /// empty list if no such column exists.
  ///
  /// @param colIndex the index of the column
  /// @return the values in the column
  default List<T> columnValues(C colIndex) {
    return column(colIndex).values().stream().toList();
  }

  /// Returns an unmodifiable view of the columns of this table.
  ///
  /// @return the columns of this table
  default List<SequencedMap<R, T>> columns() {
    return colIndexes().stream().map(this::column).toList();
  }

  /// Returns a new unmodifiable table built by expanding the column at `colIndex` using the
  /// `expander` function. The new table will have the same row indexes of this table.
  ///
  /// @param colIndex the index of the column to expand
  /// @param expander the function to apply to expand each column value
  /// @param <C1>     the type of the new table column indexes
  /// @param <T1>     the type of the new table values
  /// @return the new unmodifiable table
  default <C1, T1> Table<R, C1, T1> expandColumn(
      C colIndex,
      BiFunction<R, T, SequencedMap<C1, T1>> expander
  ) {
    return fromRows(
        rowIndexes().stream()
            .map(ri -> new Series<>(ri, expander.apply(ri, get(ri, colIndex))))
            .toList()
    );
  }

  /// Returns a new unmodifiable table built by expanding the row indexes using the `expander`
  /// function. The new table will have the same row indexes of this table.
  ///
  /// @param expander the function to apply to expand each row index
  /// @param <C1>     the type of the new table column indexes
  /// @param <T1>     the type of the new table values
  /// @return the new unmodifiable table
  default <C1, T1> Table<R, C1, T1> expandRowIndex(Function<R, SequencedMap<C1, T1>> expander) {
    return fromRows(
        rowIndexes().stream()
            .map(ri -> new Series<>(ri, expander.apply(ri)))
            .toList()
    );
  }

  /// Returns a new unmodifiable table that contains all and only the columns of this table which
  /// match the provided `colPredicate`.
  ///
  /// @param colPredicate the predicate to filter columns
  /// @return the new unmodifiable table
  default Table<R, C, T> filterColumns(Predicate<Series<C, R, T>> colPredicate) {
    return fromColumns(
        colIndexes().stream()
            .map(ci -> new Series<>(ci, column(ci)))
            .filter(colPredicate)
            .toList()
    );
  }

  /// Returns a new unmodifiable table that contains all and only the rows of this table for which
  /// the value at the column `colIndex` matches the provided `predicate`.
  ///
  /// @param predicate the predicate to filter rows
  /// @return the new unmodifiable table
  default Table<R, C, T> filterColumnsByRowValue(R rowIndex, Predicate<T> predicate) {
    return filterColumnsByValues(c -> predicate.test(c.get(rowIndex)));
  }

  /// Returns a new unmodifiable table that contains all and only the rows of this table whose
  /// values match the provided `predicate`.
  ///
  /// @param predicate the predicate to filter rows
  /// @return the new unmodifiable table
  default Table<R, C, T> filterColumnsByValues(Predicate<SequencedMap<R, T>> predicate) {
    return filterColumns(c -> predicate.test(c.values));
  }

  /// Returns a new unmodifiable table that contains all and only the rows of this table which match
  /// the provided `rowPredicate`.
  ///
  /// @param rowPredicate the predicate to filter rows
  /// @return the new unmodifiable table
  default Table<R, C, T> filterRows(Predicate<Series<R, C, T>> rowPredicate) {
    return fromRows(
        rowIndexes().stream()
            .map(ri -> new Series<>(ri, row(ri)))
            .filter(rowPredicate)
            .toList()
    );
  }

  /// Returns a new unmodifiable table that contains all and only the rows of this table for which
  /// the value at the column `colIndex` matches the provided `predicate`.
  ///
  /// @param predicate the predicate to filter rows
  /// @return the new unmodifiable table
  default Table<R, C, T> filterRowsByColumnValue(C colIndex, Predicate<T> predicate) {
    return filterRowsByValues(r -> predicate.test(r.get(colIndex)));
  }

  /// Returns a new unmodifiable table that contains all and only the rows of this table whose
  /// values match the provided `predicate`.
  ///
  /// @param predicate the predicate to filter rows
  /// @return the new unmodifiable table
  default Table<R, C, T> filterRowsByValues(Predicate<SequencedMap<C, T>> predicate) {
    return filterRows(row -> predicate.test(row.values));
  }

  /// Returns the value at the cell given by the provided row and column indexes, if any. Returns
  /// `null` if the cell value is actually `null` or if the table does not have a column at
  /// `colIndex` or a row at `rowIndex`.
  ///
  /// @param rowIndex the index of the row of the cell
  /// @param colIndex the index of the column of the cell
  /// @return the value at the cell given by the provided row and column indexes, or `null` if no
  ///  value
  T get(R rowIndex, C colIndex);

  /// Returns the value at the cell given by the provided column sequential index `x` and row
  /// sequential index `y`, if any. Returns `null` if the cell value is actually `null`; throws an
  /// exception if `x`,`y` is not valid, i.e., if the table has fewer than `x`+1 columns or fewer
  /// than `y`+1 rows or if `x` or `y` are negative.
  ///
  /// @param x the column sequential index
  /// @param y the row sequential index
  /// @return the value at the cell, or `null` if no  value
  /// @throws IndexOutOfBoundsException if `x`,`y` is not valid
  default T get(int x, int y) {
    if (x >= colIndexes().size() || x < 0 || y >= rowIndexes().size() || y < 0) {
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
    R ri = rowIndexes().stream().skip(y).findFirst().orElseThrow();
    C ci = colIndexes().stream().skip(x).findFirst().orElseThrow();
    return get(ri, ci);
  }

  /// Returns a new unmodifiable table built by applying the `mapper` to row indexes of this table.
  /// The new table will have the same column indexes of this table and the same values; but could
  /// have, in general fewer columns, if `mapper` is not injective for row indexes of this table.
  ///
  /// @param mapper the function to map row indexes to new row indexes
  /// @param <R1>   the type of the new table row indexes
  /// @return the new unmodifiable table
  default <R1> Table<R1, C, T> mapRowIndexes(BiFunction<R, SequencedMap<C, T>, R1> mapper) {
    return fromRows(
        rowIndexes().stream()
            .map(ri -> new Series<>(mapper.apply(ri, row(ri)), row(ri)))
            .toList()
    );
  }

  /// Returns a new unmodifiable table built by applying the `mapper` to row indexes of this table.
  /// The new table will have the same column indexes of this table and the same values; but could
  /// have, in general fewer columns, if `mapper` is not injective for row indexes of this table.
  ///
  /// @param mapper the function to map row indexes to new row indexes
  /// @param <R1>   the type of the new table row indexes
  /// @return the new unmodifiable table
  default <R1> Table<R1, C, T> mapRowIndexes(Function<R, R1> mapper) {
    return mapRowIndexes((ri, values) -> mapper.apply(ri));
  }

  /// Returns a new unmodifiable table built by applying the `mapper` lazily and cell-wise to this
  /// table. The new table will have the same row indexes and column indexes of this table. The
  /// `mapper` is actually applied to a cell value only if it is actually retrieved (with, e.g.,
  /// [#get(Object, Object)]), at the first retrieval. This makes the built table potentially a view
  /// of the original (this) table.
  ///
  /// @param mapper the function to map cells to new cell values
  /// @param <T1>   the type of the new table values
  /// @return the new unmodifiable table
  default <T1> Table<R, C, T1> mapValues(TriFunction<R, C, T, T1> mapper) {
    Map<Pair<R, C>, T1> map = new HashMap<>();
    return Unmodifiable.of(
        rowIndexes(),
        colIndexes(),
        (ri, ci) -> map.computeIfAbsent(
            new Pair<>(ri, ci),
            lCoords -> mapper.apply(
                lCoords.first(),
                lCoords.second(),
                get(lCoords.first(), lCoords.second())
            )
        )
    );
  }

  /// Returns a new unmodifiable table built by applying the `mapper` lazily and cell-wise to this
  /// table. The new table will have the same row indexes and column indexes of this table. The
  /// `mapper` is actually applied to a cell value only if it is actually retrieved (with, e.g.,
  /// [#get(Object, Object)]), at the first retrieval. This makes the built table potentially a view
  /// of the original (this) table.
  ///
  /// @param mapper the function to map cell values to new cell values
  /// @param <T1>   the type of the new table values
  /// @return the new unmodifiable table
  default <T1> Table<R, C, T1> mapValues(Function<T, T1> mapper) {
    return mapValues((ri, ci, t) -> mapper.apply(t));
  }

  /// Returns the number of columns in this table.
  ///
  /// @return the number of columns in this table.
  default int nOfColumns() {
    return colIndexes().size();
  }

  /// Returns the number of rows in this table.
  ///
  /// @return the number of rows in this table.
  default int nOfRows() {
    return rowIndexes().size();
  }

  /// Returns a human-readable representation of this table, in a way one would expect to see a
  /// table. The returned string contains $n+1$ (with $n$ being the number of rows) lines, the first
  /// one for the header, then one for each row. The content of the lines is formatted to make cell
  /// values and the initial row index aligned. Individual values, row indexes, and col indexes are
  /// transformed to strings through the provided formatted functions. Those functions are expected
  /// not to return multi-line strings. One convenient way to provide the formatters is through
  /// method reference to the [String#formatted(Object...)] method, e.g.:
  /// ```java
  /// Table<Integer, String, Double> table = /* ... */
  /// System.out.println(table.prettyToString(
  ///   "%d"::formatted,
  ///   "%s"::formatted,
  ///   "%.3"::formatted
  ///));
  ///```
  ///
  /// @param rowIndexFormatter the formatter for row indexes
  /// @param colIndexFormatter the formatter for column indexes
  /// @param valueFormatter    the formatter for values
  /// @return a human-readable string representation of this table
  default String prettyToString(
      Function<R, String> rowIndexFormatter,
      Function<C, String> colIndexFormatter,
      Function<T, String> valueFormatter
  ) {
    if (nOfColumns() == 0) {
      return "";
    }
    String colSep = " ";
    Map<C, Integer> widths = colIndexes().stream()
        .collect(
            Collectors.toMap(
                ci -> ci,
                ci -> Math.max(
                    colIndexFormatter.apply(ci).length(),
                    rowIndexes().stream()
                        .mapToInt(
                            ri -> valueFormatter.apply(get(ri, ci)).length()
                        )
                        .max()
                        .orElse(0)
                ),
                Table::first,
                LinkedHashMap::new
            )
        );
    int riWidth = rowIndexes().stream()
        .mapToInt(ri -> rowIndexFormatter.apply(ri).length())
        .max()
        .orElse(0);
    StringBuilder sb = new StringBuilder();
    // print header
    sb.append(justify("", riWidth));
    sb.append(riWidth > 0 ? colSep : "");
    sb.append(
        colIndexes().stream()
            .map(ci -> justify(colIndexFormatter.apply(ci), widths.get(ci)))
            .collect(Collectors.joining(colSep))
    );
    if (nOfRows() == 0) {
      return sb.toString();
    }
    sb.append("\n");
    // print rows
    sb.append(
        rowIndexes().stream()
            .map(ri -> {
              String s = justify(rowIndexFormatter.apply(ri), riWidth);
              s = s + (riWidth > 0 ? colSep : "");
              s = s + colIndexes().stream()
                  .map(ci -> justify(valueFormatter.apply(get(ri, ci)), widths.get(ci)))
                  .collect(Collectors.joining(colSep));
              return s;
            })
            .collect(Collectors.joining("\n"))
    );
    return sb.toString();
  }

  /// Returns a human-readable representation of this table, in a way one would expect to see a
  /// table. The returned string contains $n+1$ (with $n$ being the number of rows) lines, the first
  /// one for the header, then one for each row. The content of the lines is formatted to make cell
  /// values and the initial row index aligned. Individual values, row indexes, and col indexes are
  /// transformed to strings through [Object#toString()].
  ///
  /// @return a human-readable string representation of this table
  default String prettyToString() {
    return prettyToString(Objects::toString, Objects::toString, Objects::toString);
  }

  /// Removes the column at `colIndex`, if any, from this table. If the table has no column at
  /// `colIndex`, invoking this method has no effect.
  ///
  /// @param colIndex the index of the column to remove
  void removeColumn(C colIndex);

  /// Removes the row at `rowIndex`, if any, from this table. If the table has no row at `rowIndex`,
  /// invoking this method has no effect.
  ///
  /// @param rowIndex the index of the column to remove
  void removeRow(R rowIndex);

  /// Returns an unmodifiable view of the row at `rowIndex` of this table, or an empty map if no
  /// such row exists.
  ///
  /// @param rowIndex the index of the column
  /// @return a map with the values in the cells of the row indexed by column indexes
  default SequencedMap<C, T> row(R rowIndex) {
    if (!rowIndexes().contains(rowIndex)) {
      return Collections.unmodifiableSequencedMap(new LinkedHashMap<>());
    }
    return Collections.unmodifiableSequencedMap(
        (SequencedMap<? extends C, ? extends T>) colIndexes().stream()
            .filter(ci -> get(rowIndex, ci) != null)
            .collect(
                Collectors.toMap(
                    ci -> ci,
                    ci -> get(rowIndex, ci),
                    Table::first,
                    LinkedHashMap::new
                )
            )
    );
  }

  /// Returns the row indexes.
  ///
  /// @return the row indexes
  SequencedSet<R> rowIndexes();

  /// Returns an unmodifiable view of the values in the row at `rowIndex` of this table, or an empty
  /// list if no such row exists.
  ///
  /// @param rowIndex the index of the row
  /// @return the values in the row
  default List<T> rowValues(R rowIndex) {
    return row(rowIndex).values().stream().toList();
  }

  /// Returns an unmodifiable view of the rows of this table.
  ///
  /// @return the rows of this table
  default List<SequencedMap<C, T>> rows() {
    return rowIndexes().stream().map(this::row).toList();
  }

  /// Set the value at the cell given by the provided row and column indexes. If there is not a row
  /// at `rowIndex`, adds the row as last row. If there is not a column at `colIndex`, adds the
  /// column as last column.
  ///
  /// @param rowIndex the index of the row of the cell
  /// @param colIndex the index of the column of the cell
  void set(R rowIndex, C colIndex, T t);

  /// Set the value at the cell given by the provided column sequential index `x` and row sequential
  /// index `y`, if any. Throws an exception if `x`,`y` is not valid, i.e., if the table has fewer
  /// than `x`+1 columns or fewer than `y`+1 rows or if `x` or `y` are negative.
  ///
  /// @param x the column sequential index
  /// @param y the row sequential index
  /// @throws IndexOutOfBoundsException if `x`,`y` is not valid
  default void set(int x, int y, T t) {
    if (x >= colIndexes().size() || x < 0 || y >= rowIndexes().size() || y < 0) {
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
    R ri = rowIndexes().stream().skip(y).findFirst().orElseThrow();
    C ci = colIndexes().stream().skip(x).findFirst().orElseThrow();
    set(ri, ci, t);
  }

  /// Returns a values-only view of this table where rows are sorted according to the provided
  /// `comparator`. A values-only view is one where values in the cells reflect the changes of the
  /// original (this) table, but the structure of the view table is the one at creation. That is,
  /// row indexes and col indexes stay the same in values and order once the view is created.
  ///
  /// @param comparator the comparator for ordering row indexes
  /// @return the values-only view of this table
  default Unmodifiable<R, C, T> sortedByRowIndex(Comparator<R> comparator) {
    return Unmodifiable.of(
        rowIndexes().stream()
            .sorted(comparator)
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        colIndexes(),
        this::get
    );
  }

  /// Returns a new unmodifiable where rows are sorted according to the provided `comparator`
  /// applied to values of the column at `colIndex`.
  ///
  /// @param comparator the comparator for ordering values
  /// @return the values-only view of this table
  default Unmodifiable<R, C, T> sortedByValue(C colIndex, Comparator<T> comparator) {
    Map<R, Map<C, T>> map = rowIndexes().stream()
        .collect(
            Collectors.toMap(
                ri -> ri,
                this::row
            )
        );
    return Unmodifiable.of(
        rowIndexes().stream()
            .sorted(
                Comparator.comparing(ri -> row(ri).get(colIndex), comparator)
            )
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        colIndexes(),
        (ri, ci) -> map.getOrDefault(ri, Map.of()).get(ci)
    );
  }

  /// Returns a list containing all the values of this table, by concatenating the values from the
  /// first to the last row.
  ///
  /// @return all the values of this table
  default List<T> values() {
    return rowIndexes().stream()
        .flatMap(ri -> colIndexes().stream().map(ci -> get(ri, ci)))
        .toList();
  }

  /// Returns a values-only view of this table which has one column at `newColIndex` containing the
  /// provided `newValue` at each row. A values-only view is one where values in the cells reflect
  /// the changes of the original (this) table, but the structure of the view table is the one at
  /// creation (with the new column). The new column possibly overwrites (in the view) an existing
  /// column with the same column index in this table.
  ///
  /// @param newColIndex the index of the new column
  /// @param newT        the value in each cell of the new column
  /// @return the values-only view of this table
  default Unmodifiable<R, C, T> with(C newColIndex, T newT) {
    SequencedSet<C> colIndexes = new LinkedHashSet<>(colIndexes());
    colIndexes.add(newColIndex);
    return Unmodifiable.of(
        rowIndexes(),
        colIndexes,
        (ri, ci) -> {
          if (ci.equals(newColIndex)) {
            return newT;
          }
          return get(ri, ci);
        }
    );
  }

  /// A read-only table for which all the methods for modifying the content throw an
  /// [UnsupportedOperationException]. Many methods of [Table] return views which are read-only
  /// tables.
  ///
  /// @param <R> the type of row indexes
  /// @param <C> the type of column indexes
  /// @param <T> the type of values in the cells
  interface Unmodifiable<R, C, T> extends Table<R, C, T> {

    /// Creates a new unmodifiable table based on the provided `rowIndexes`, `colIndexes`, and
    /// `retriever`. The `retriever` is used for retrieve table values; the returned table does not
    /// actually store values, but instead retrieve them through `retriever`. The `retriever` is
    /// only invoked if both row and column indexes are contained in the provided``rowIndexes` and
    ///`colIndexes`.
    ///
    /// @param rowIndexes the row indexes of the new unmodifiable table
    /// @param colIndexes the col indexes of the new unmodifiable table
    /// @param retriever  the function used to retrieve the value of the
    /// @param <R>        the type of row indexes
    /// @param <C>        the type of column indexes
    /// @param <T>        the type of values in the cells
    /// @return the new unmodifiable table
    static <R, C, T> Unmodifiable<R, C, T> of(
        SequencedSet<R> rowIndexes,
        SequencedSet<C> colIndexes,
        BiFunction<R, C, T> retriever
    ) {
      return new Unmodifiable<>() {
        @Override
        public SequencedSet<C> colIndexes() {
          return colIndexes;
        }

        @Override
        public T get(R rowIndex, C colIndex) {
          if (rowIndexes.contains(rowIndex) && colIndexes.contains(colIndex)) {
            return retriever.apply(rowIndex, colIndex);
          }
          return null;
        }

        @Override
        public SequencedSet<R> rowIndexes() {
          return rowIndexes;
        }
      };
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param column the new column
    /// @throws UnsupportedOperationException always
    @Override
    default void addColumn(Series<C, R, T> column) {
      throw new UnsupportedOperationException("This is a read only table");
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param row the new row
    /// @throws UnsupportedOperationException always
    @Override
    default void addRow(Series<R, C, T> row) {
      throw new UnsupportedOperationException("This is a read only table");
    }

    /// Throws an exception, as the table is read-only.
    ///
    /// @param colIndex the index of the column to be removed
    /// @throws UnsupportedOperationException always
    @Override
    default void removeColumn(C colIndex) {
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
    /// @param rowIndex the row index of cell where to set the value
    /// @param colIndex the column index of cell where to set the value
    /// @param t        the new value
    /// @throws UnsupportedOperationException always
    @Override
    default void set(R rowIndex, C colIndex, T t) {
      throw new UnsupportedOperationException("This is a read only table");
    }
  }

  /// A series of values of tells, representing either a row or a column. If used to represent a
  /// row, the `primaryIndex` is the row index; otherwise, it is the column index.
  ///
  /// @param primaryIndex the row/column index of the series
  /// @param values       the values in the series
  /// @param <P>          the type of the primary (row/column) index
  /// @param <S>          the type of the secondary (column/row) index
  /// @param <T>          the type of values in the cells
  record Series<P, S, T>(P primaryIndex, SequencedMap<S, T> values) {

  }
}
