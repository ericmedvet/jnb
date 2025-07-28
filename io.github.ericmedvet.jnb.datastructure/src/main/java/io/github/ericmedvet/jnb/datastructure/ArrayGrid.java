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

import java.io.Serializable;
import java.util.Arrays;

/// An implementation of [Grid] which internally stores the elements with an array.
///
/// @param <T> the type of cell values
public class ArrayGrid<T> extends AbstractGrid<T> implements Serializable {

  private final Object[] ts;

  /// Builds an empty grid with the provided width and height.
  ///
  /// @param w the width of the grid
  /// @param h the height of the grid
  public ArrayGrid(int w, int h) {
    super(w, h);
    this.ts = new Object[w * h];
  }

  @Override
  public T get(Key key) {
    checkValidity(key);
    //noinspection unchecked
    return (T) ts[(key.y() * w()) + key.x()];
  }

  @Override
  public void set(Key key, T t) {
    checkValidity(key);
    ts[(key.y() * w()) + key.x()] = t;
  }

  /// Returns the hash code value for this grid.
  ///
  /// @return the hash code value for this grid
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(ts);
    return result;
  }

  /// Compares the specified object with this grid for equality. Two grids are considered equal if
  /// both the following condition hold:
  /// - they have the same width and height
  /// - for each cell, the elements in the two grid are either `null` or equal
  ///
  /// @param o the object to be compared for equality with this list
  /// @return true if the specified object is equal to this grid, false otherwise
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ArrayGrid<?> arrayGrid = (ArrayGrid<?>) o;
    return Arrays.equals(ts, arrayGrid.ts);
  }
}
