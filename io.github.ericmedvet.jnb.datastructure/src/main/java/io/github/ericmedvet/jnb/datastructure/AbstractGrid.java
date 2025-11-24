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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A partly abstract implementation of [Grid]. It provides the concrete implementation of a few
/// methods, but does not specify how data is stored. Internally, this implementation stores the
/// keys (coordinates) of the grid.
///
/// @param <T> the type of cell values
public abstract class AbstractGrid<T> implements Grid<T> {

  private final int w;
  private final int h;
  private final List<Key> keys;

  /// Constructor to call by subclasses.
  ///
  /// @param w the width of the grid
  /// @param h the height of the grid
  protected AbstractGrid(int w, int h) {
    this.w = w;
    this.h = h;
    List<Key> localKeys = new ArrayList<>(w() * h());
    for (int x = 0; x < w(); x++) {
      for (int y = 0; y < h(); y++) {
        localKeys.add(new Key(x, y));
      }
    }
    keys = Collections.unmodifiableList(localKeys);
  }

  /// Checks if the provided coordinate `key` is valid based on this grid width and height and
  /// throws an exception if it is not. Works like [Grid#isValid(Key)] (which this method internally
  /// calls), but the outcome is a possible exception, rather than a Boolean.
  ///
  /// @param key the coordinate to test for validity
  /// @throws IllegalArgumentException if `key` is not valid
  protected void checkValidity(Key key) {
    if (!isValid(key)) {
      throw new IllegalArgumentException(
          "Invalid coords (%d,%d) on a %dx%d grid".formatted(key.x(), key.y(), w(), h())
      );
    }
  }

  @Override
  public int h() {
    return h;
  }

  @Override
  public int w() {
    return w;
  }

  @Override
  public List<Key> keys() {
    return keys;
  }

  /// Returns a simple string representation of this grid.
  ///
  /// @return a simple string representation of this grid
  @Override
  public String toString() {
    return "%s(%dx%d)%s".formatted(getClass().getSimpleName(), w(), h(), entries());
  }
}
