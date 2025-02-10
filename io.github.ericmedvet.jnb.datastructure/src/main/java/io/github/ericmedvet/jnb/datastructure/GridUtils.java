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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/// This class consists of `static` utility methods for operating on grids, i.e., objects of type `Grid`.
public class GridUtils {

  private static final int ELONGATION_STEPS = 20;

  private GridUtils() {
  }

  /// Returns the center of gravity of the provided `grid` values which match the provided `predicate`.
  /// The center of gravity is computed as the grid coordinate where `x` is the average `x` of cells with values
  /// matching the predicate and `y` is the average `y` of cells with values matching the predicate.
  /// The computed average values are rounded to the closest integer.
  ///
  /// @param grid      the grid to compute the center of gravity of
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the coordinate of the center of gravity
  public static <T> Grid.Key center(Grid<T> grid, Predicate<T> predicate) {
    return new Grid.Key(
        (int) Math.round(
            grid.entries()
                .stream()
                .filter(e -> predicate.test(e.value()))
                .mapToInt(e -> e.key().x())
                .average()
                .orElse(0d)
        ),
        (int) Math.round(
            grid.entries()
                .stream()
                .filter(e -> predicate.test(e.value()))
                .mapToInt(e -> e.key().y())
                .average()
                .orElse(0d)
        )
    );
  }

  /// Returns a measure of the compactness of the Boolean grid obtained by testing the provided `grid` values against
  ///  the provided `predicate`.
  /// The compactness is computed as the ratio between the number of true values in the Boolean grid and the area
  /// (i.e., number of cells) of the smallest convex hull enclosing the true values of the Boolean grid.
  /// The compactness is theoretically defined in $\[0,1\]$, with 0 meaning a non-compact Boolean grid and 1 meaning
  /// a fully compact Boolean grid.
  ///
  /// @param grid      the grid to compute the compactness of
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the compactness of the Boolean grid obtained by testing `grid` values against `predicate`
  public static <T> double compactness(Grid<T> grid, Predicate<T> predicate) {
    // approximate convex hull
    Grid<Boolean> convexHull = grid.map(predicate::test);
    boolean none = false;
    // loop as long as there are false cells have at least five of the eight Moore neighbors as true
    while (!none) {
      none = true;
      for (Grid.Entry<Boolean> entry : convexHull) {
        if (convexHull.get(entry.key().x(), entry.key().y())) {
          continue;
        }
        int currentX = entry.key().x();
        int currentY = entry.key().y();
        int adjacentCount = 0;
        // count how many of the Moore neighbors are true
        for (int i : new int[]{1, -1}) {
          int neighborX = currentX;
          int neighborY = currentY + i;
          if (0 <= neighborY && neighborY < convexHull.h() && convexHull.get(neighborX, neighborY)) {
            adjacentCount += 1;
          }
          neighborX = currentX + i;
          neighborY = currentY;
          if (0 <= neighborX && neighborX < convexHull.w() && convexHull.get(neighborX, neighborY)) {
            adjacentCount += 1;
          }
          neighborX = currentX + i;
          neighborY = currentY + i;
          if (0 <= neighborX && 0 <= neighborY && neighborX < convexHull.w() && neighborY < convexHull.h() && convexHull
              .get(neighborX, neighborY)) {
            adjacentCount += 1;
          }
          neighborX = currentX + i;
          neighborY = currentY - i;
          if (0 <= neighborX && 0 <= neighborY && neighborX < convexHull.w() && neighborY < convexHull.h() && convexHull
              .get(neighborX, neighborY)) {
            adjacentCount += 1;
          }
        }
        // if at least five, fill the cell
        if (adjacentCount >= 5) {
          convexHull.set(entry.key().x(), entry.key().y(), true);
          none = false;
        }
      }
    }
    // compute are ratio between convex hull and posture
    double nVoxels = count(grid, predicate);
    double nConvexHull = count(convexHull, b -> b);
    // -> 0.0 for less compact shapes, -> 1.0 for more compact shapes
    return nVoxels / nConvexHull;
  }

  /// Returns the number of values of the provided `grid` which match the provided `predicate`.
  ///
  /// @param grid      the grid to count the values of
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the number of values matching the predicate
  public static <T> int count(Grid<T> grid, Predicate<T> predicate) {
    return (int) grid.values().stream().filter(predicate).count();
  }

  /// Returns a measure of the elongation of the Boolean grid obtained by testing the provided `grid` values against
  ///  the provided `predicate`.
  /// The elongation is computed as one minus the ratio between the shortest and the longest diameter of the Boolean
  /// grid.
  /// The elongation is theoretically defined in $\[0,1\]$, with 0 meaning a very elongated Boolean grid and 1 meaning
  /// a perfectly circular Boolean grid.
  /// This method actually computes an estimate of the elongation by computing the ratio of two perpendicular
  /// diameters sampled at {@value #ELONGATION_STEPS} different equispaced angles.
  ///
  /// @param grid      the grid to compute the elongation of
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the elongation of the Boolean grid obtained by testing `grid` values against `predicate`
  /// @throws IllegalArgumentException if the grid has no values matching the predicate or if `n` is not positive
  public static <T> double elongation(Grid<T> grid, Predicate<T> predicate) {
    return elongation(grid, predicate, ELONGATION_STEPS);
  }

  /// Returns a measure of the elongation of the Boolean grid obtained by testing the provided `grid` values against
  ///  the provided `predicate`.
  /// The elongation is computed as one minus the ratio between the shortest and the longest diameter of the Boolean
  /// grid.
  /// The elongation is theoretically defined in $\[0,1\]$, with 0 meaning a very elongated Boolean grid and 1 meaning
  /// a perfectly circular Boolean grid.
  /// This method actually computes an estimate of the elongation by computing the ratio of two perpendicular
  /// diameters sampled at `n` different equispaced angles: the larger `n`, the more accurate the estimate.
  ///
  /// @param grid      the grid to compute the elongation of
  /// @param predicate the predicate to test values
  /// @param n         the number of angle samples to compute the diameters ratios on
  /// @param <T>       the type of cell values
  /// @return the elongation of the Boolean grid obtained by testing `grid` values against `predicate`
  /// @throws IllegalArgumentException if the grid has no values matching the predicate or if `n` is not positive
  public static <T> double elongation(Grid<T> grid, Predicate<T> predicate, int n) {
    if (grid.values().stream().noneMatch(predicate)) {
      throw new IllegalArgumentException("Grid is empty");
    } else if (n <= 0) {
      throw new IllegalArgumentException(String.format("Non-positive number of directions provided: %d", n));
    }
    List<Grid.Key> coordinates = grid.stream().filter(e -> predicate.test(e.value())).map(Grid.Entry::key).toList();
    List<Double> ratios = new ArrayList<>();
    for (int i = 0; i < n; ++i) {
      double theta = (2 * i * Math.PI) / n;
      List<Grid.Key> rotatedCoordinates = coordinates.stream()
          .map(
              k -> new Grid.Key(
                  (int) Math.round(k.x() * Math.cos(theta) - k.y() * Math.sin(theta)),
                  (int) Math.round(k.x() * Math.sin(theta) + k.y() * Math.cos(theta))
              )
          )
          .toList();
      double minX = rotatedCoordinates.stream()
          .min(Comparator.comparingInt(Grid.Key::x))
          .orElseThrow()
          .x();
      double maxX = rotatedCoordinates.stream()
          .max(Comparator.comparingInt(Grid.Key::x))
          .orElseThrow()
          .x();
      double minY = rotatedCoordinates.stream()
          .min(Comparator.comparingInt(Grid.Key::y))
          .orElseThrow()
          .y();
      double maxY = rotatedCoordinates.stream()
          .max(Comparator.comparingInt(Grid.Key::y))
          .orElseThrow()
          .y();
      double sideX = maxX - minX + 1;
      double sideY = maxY - minY + 1;
      ratios.add(Math.min(sideX, sideY) / Math.max(sideX, sideY));
    }
    return 1.0 - Collections.min(ratios);
  }

  /// Returns the smallest grid which contains all the values of the provided `grid` which match the provided `
  /// predicate`.
  /// The returned grid is in general smaller or of the same size of the provided `grid` and is made in such a way that:
  /// - the lowest `x` coordinate of cells with values matching `predicate` is 0;
  /// - the largest `x` coordinate of cells with values matching `predicate` is `w`-1 (`w` being the width of the new
  ///  grid);
  /// - the lowest `y` coordinate of cells with values matching `predicate` is 0;
  /// - the largest `y` coordinate of cells with values matching `predicate` is `h`-1 (`h` being the height of the
  /// new grid).
  ///
  /// In other words, the new grid size fits the provided grid values.
  ///
  /// For example, consider the following `Grid<Integer>` with width 5 and height 4 (and whose values are all in
  /// $\[0,9\]$):
  /// ```
  /// 01200
  /// 00000
  /// 00340
  /// 00567
  ///```
  /// then `fit(grid, v -> v > 3)` would return:
  /// ```
  /// 340
  /// 567
  ///```
  ///
  /// @param grid      the grid to adapt into a new grid
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the new grid which fits the provided grid values
  public static <T> Grid<T> fit(Grid<T> grid, Predicate<T> predicate) {
    int minX = grid.entries()
        .stream()
        .filter(e -> predicate.test(e.value()))
        .mapToInt(e -> e.key().x())
        .min()
        .orElseThrow();
    int maxX = grid.entries()
        .stream()
        .filter(e -> predicate.test(e.value()))
        .mapToInt(e -> e.key().x())
        .max()
        .orElseThrow();
    int minY = grid.entries()
        .stream()
        .filter(e -> predicate.test(e.value()))
        .mapToInt(e -> e.key().y())
        .min()
        .orElseThrow();
    int maxY = grid.entries()
        .stream()
        .filter(e -> predicate.test(e.value()))
        .mapToInt(e -> e.key().y())
        .max()
        .orElseThrow();
    return Grid.create(maxX - minX + 1, maxY - minY + 1, (x, y) -> grid.get(x + minX, y + minY));
  }

  /// Returns the height of the smallest grid enclosing all the cell of the provided `grid` whose values match the
  /// provided `predicate` (the matching subgrid).
  /// Internally calls [#fit(Grid, Predicate)].
  ///
  /// @param grid      the grid to compute the height of matching subgrid of
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the height of the matching subgrid
  public static <T> int h(Grid<T> grid, Predicate<T> predicate) {
    return fit(grid, predicate).h();
  }

  /// Returns a robust version of the [Hamming distance](https://en.wikipedia.org/wiki/Hamming_distance) between two
  /// grids, possibly aligning them.
  /// The distance operates on values and basically gives the number of coordinates for which the values in the
  /// corresponding cells of the two grids are not equal.
  /// The distance is robust in the sense that it works even if the two grids have different sizes, assuming as
  /// `null` values at cells out of the grid.
  /// If `align` is true, before computing the distance, the two grid centers (computed with
  ///  [#center(Grid, Predicate)] and [Objects#nonNull(Object)] as predicate) are aligned.
  ///
  /// @param grid1 the first grid
  /// @param grid2 the second grid
  /// @param align a flag specifying if aligning grid centers before computing the distance
  /// @param <T>   the type of cell values
  /// @return the Hamming distance between `grid1` and `grid2`
  public static <T> int hammingDistance(Grid<T> grid1, Grid<T> grid2, boolean align) {
    if (align) {
      Grid.Key g1Center = center(grid1, Objects::nonNull);
      Grid.Key g2Center = center(grid2, Objects::nonNull);
      Grid.Key newCenter = new Grid.Key(Math.max(g1Center.x(), g2Center.x()), Math.max(g1Center.y(), g2Center.y()));
      grid1 = GridUtils.translate(grid1, new Grid.Key(newCenter.x() - g1Center.x(), newCenter.y() - g1Center.y()));
      grid2 = GridUtils.translate(grid2, new Grid.Key(newCenter.x() - g2Center.x(), newCenter.y() - g2Center.y()));
    }
    int sum = 0;
    for (int x = 0; x < Math.max(grid1.w(), grid2.w()); x = x + 1) {
      for (int y = 0; y < Math.max(grid1.h(), grid2.h()); y = y + 1) {
        Grid.Key k = new Grid.Key(x, y);
        sum = sum + (safeGet(grid1, k).equals(safeGet(grid2, k)) ? 0 : 1);
      }
    }
    return sum;
  }

  /// Returns a grid with the same size of the provided `grid` that contains only the values of the largest subset of
  ///  `grid` values which are connected (i.e., belonging to the [von Neumann neighborhood](https://en.wikipedia
  /// .org/wiki/Von_Neumann_neighborhood) and matching the provided `predicate`) to each other, and `emptyT` elsewhere.
  /// For example, consider the following `Grid<Character>` with width 5 and height 4:
  /// ```
  /// -12--
  /// -----
  /// --34-
  /// --567
  ///```
  /// then `largestConnected(grid, Character::isDigit, '.')` would return:
  /// ```
  /// .....
  /// .....
  /// ..34.
  /// ..567
  ///```
  ///
  /// @param grid      the grid to take the largest connected subgrid from
  /// @param predicate the predicate to test values
  /// @param emptyT    the value to put in cells of `grid` remaining or being empty
  /// @param <T>       the type of cell values
  /// @return the equally sized grid containing only the elements of the largest connected subgrid
  public static <T> Grid<T> largestConnected(Grid<T> grid, Predicate<T> predicate, T emptyT) {
    Grid<Integer> iGrid = partitionGrid(grid, predicate);
    // count elements per partition
    Map<Integer, Integer> counts = new LinkedHashMap<>();
    for (Integer i : iGrid.values()) {
      if (i != null) {
        counts.put(i, counts.getOrDefault(i, 0) + 1);
      }
    }
    // find largest
    Integer maxIndex = counts.entrySet()
        .stream()
        .max(Comparator.comparingInt(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .orElse(null);
    // filter map
    return grid.map((k, t) -> (iGrid.get(k) != null && iGrid.get(k).equals(maxIndex)) ? grid.get(k) : emptyT);
  }

  private static <T> Grid<Integer> partitionGrid(Grid<T> kGrid, Predicate<T> p) {
    Grid<Integer> iGrid = Grid.create(kGrid.w(), kGrid.h());
    for (int x = 0; x < kGrid.w(); x++) {
      for (int y = 0; y < kGrid.h(); y++) {
        if (iGrid.get(x, y) == null) {
          int index = iGrid.values()
              .stream()
              .filter(Objects::nonNull)
              .mapToInt(i -> i)
              .max()
              .orElse(0);
          partitionGrid(x, y, index + 1, kGrid, iGrid, p);
        }
      }
    }
    return iGrid;
  }

  private static <T> void partitionGrid(int x, int y, int i, Grid<T> kGrid, Grid<Integer> iGrid, Predicate<T> p) {
    boolean hereFilled = p.test(kGrid.get(x, y));
    // already done
    if (iGrid.get(x, y) != null) {
      return;
    }
    // filled but not done
    if (hereFilled) {
      iGrid.set(x, y, i);
      // expand east
      if (x > 0) {
        partitionGrid(x - 1, y, i, kGrid, iGrid, p);
      }
      // expand west
      if (x < kGrid.w() - 1) {
        partitionGrid(x + 1, y, i, kGrid, iGrid, p);
      }
      // expand north
      if (y > 0) {
        partitionGrid(x, y - 1, i, kGrid, iGrid, p);
      }
      // expand south
      if (y < kGrid.h() - 1) {
        partitionGrid(x, y + 1, i, kGrid, iGrid, p);
      }
    }
  }

  private static <T> Optional<T> safeGet(Grid<T> grid, Grid.Key key) {
    if (!grid.isValid(key)) {
      return Optional.empty();
    }
    T t = grid.get(key);
    return t == null ? Optional.empty() : Optional.of(t);
  }

  /// Returns a new grid obtained by "translating" the provided `grid` values by `deltaKey.x()` cells on the x-axis
  /// and `deltaKey.y()` cells on the y-axis.
  /// For each of the two axes, if the delta is negative, the new grid will be smaller along that axis; otherwise, if
  ///  it is positive, it will be larger.
  /// If new cells are needed (because the delta is positive along at least one axis), the corresponding values are
  /// set to `null` in the returned grid.
  ///
  /// @param grid     the grid to translate
  /// @param deltaKey the delta (offset) along the two axes
  /// @param <T>      the type of cell values
  /// @return the new grid obtained by translating the provided `grid`
  public static <T> Grid<T> translate(Grid<T> grid, Grid.Key deltaKey) {
    Grid<T> translated = new HashMapGrid<>(grid.w() + deltaKey.x(), grid.h() + deltaKey.y());
    grid.keys().forEach(k -> {
      if (translated.isValid(k.translated(deltaKey))) {
        translated.set(k.translated(deltaKey), grid.get(k));
      }
    });
    return translated;
  }

  /// Returns the width of the smallest grid enclosing all the cell of the provided `grid` whose values match the
  /// provided `predicate` (the matching subgrid).
  /// Internally calls [#fit(Grid, Predicate)].
  ///
  /// @param grid      the grid to compute the width of matching subgrid of
  /// @param predicate the predicate to test values
  /// @param <T>       the type of cell values
  /// @return the width of the matching subgrid
  public static <T> int w(Grid<T> grid, Predicate<T> predicate) {
    return fit(grid, predicate).w();
  }
}
