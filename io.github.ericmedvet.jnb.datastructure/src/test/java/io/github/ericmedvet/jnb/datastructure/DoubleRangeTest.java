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

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class DoubleRangeTest {

  @Test
  void intersection() {
    DoubleRange dr1 = new DoubleRange(-1, 3);
    DoubleRange dr2 = new DoubleRange(2, 5);
    DoubleRange dr3 = new DoubleRange(1, 3);
    assertThat(DoubleRange.intersection(List.of(dr1, dr2, dr3)))
        .as("min of intersection")
        .extracting(DoubleRange::min)
        .isEqualTo(2.0);
    assertThat(DoubleRange.intersection(List.of(dr1, dr2, dr3)))
        .as("max of intersection")
        .extracting(DoubleRange::max)
        .isEqualTo(3.0);
    assertThat(DoubleRange.intersection(List.of(dr1, dr2, dr3)))
        .as("invariance to order of intersection")
        .isEqualTo(DoubleRange.intersection(List.of(dr2, dr3, dr1)));
  }

  @Test
  void union() {
    DoubleRange dr1 = new DoubleRange(-1, 3);
    DoubleRange dr2 = new DoubleRange(2, 5);
    DoubleRange dr3 = new DoubleRange(1, 3);
    assertThat(DoubleRange.union(List.of(dr1, dr2, dr3)))
        .as("min of union")
        .extracting(DoubleRange::min)
        .isEqualTo(-1.0);
    assertThat(DoubleRange.union(List.of(dr1, dr2, dr3)))
        .as("max of union")
        .extracting(DoubleRange::max)
        .isEqualTo(5.0);
    assertThat(DoubleRange.union(List.of(dr1, dr2, dr3)))
        .as("invariance to order of union")
        .isEqualTo(DoubleRange.union(List.of(dr2, dr3, dr1)));
  }

  @Test
  void center() {
    assertThat(new DoubleRange(-1, 5))
        .extracting(DoubleRange::center)
        .isEqualTo(2.0);
  }

  @Test
  void clip() {
    DoubleRange dr = new DoubleRange(3, 5);
    assertThat(dr.clip(6))
        .as("clip of upper value")
        .isEqualTo(dr.max());
    assertThat(dr.clip(1))
        .as("clip of lower value")
        .isEqualTo(dr.min());
  }

  @Test
  void containsPoint() {
    DoubleRange dr = new DoubleRange(3, 5);
    assertThat(dr.contains(4))
        .as("contains inner value")
        .isTrue();
    assertThat(dr.contains(dr.min()))
        .as("contains min")
        .isTrue();
    assertThat(dr.contains(dr.max()))
        .as("contains max")
        .isTrue();
    assertThat(dr.contains(dr.max() + 1))
        .as("not contains upper value")
        .isFalse();
    assertThat(dr.contains(dr.min() - 1))
        .as("not contains lower value")
        .isFalse();
  }

  @Test
  void containsDoubleRange() {
    DoubleRange dr = new DoubleRange(3, 5);
    assertThat(dr.contains(new DoubleRange(3.5, 4.5)))
        .as("contains inner range")
        .isTrue();
    assertThat(dr.contains(new DoubleRange(6, 8)))
        .as("not contains upper range")
        .isFalse();
    assertThat(dr.contains(new DoubleRange(1, 2)))
        .as("not contains lower range")
        .isFalse();
    assertThat(dr.contains(new DoubleRange(1, 4)))
        .as("not contains overlapping range")
        .isFalse();
  }

  @Test
  void delta() {
    DoubleRange dr = new DoubleRange(3, 5);
    assertThat(dr.delta(1))
        .as("delta max")
        .extracting(DoubleRange::max)
        .isEqualTo(6.0);
    assertThat(dr.delta(-1))
        .as("delta min")
        .extracting(DoubleRange::min)
        .isEqualTo(2.0);
    assertThat(dr.delta(0))
        .as("delta with 0 is self")
        .isEqualTo(dr);
  }

  @Test
  void denormalize() {
    DoubleRange dr = new DoubleRange(3, 5);
    assertThat(dr.denormalize(0.5))
        .as("denormalized 1/2")
        .isEqualTo(dr.center());
    assertThat(dr.denormalize(0))
        .as("denormalized 0")
        .isEqualTo(dr.min());
    assertThat(dr.denormalize(1))
        .as("denormalized 1")
        .isEqualTo(dr.max());
    assertThat(dr.denormalize(-1))
        .as("denormalized <0 is min")
        .isEqualTo(dr.min());
    assertThat(dr.denormalize(3))
        .as("denormalized >1 is max")
        .isEqualTo(dr.max());
  }

  @Test
  void extend() {
    DoubleRange dr = new DoubleRange(5, 15);
    assertThat(dr.extend(2))
        .as("extended min")
        .extracting(DoubleRange::min)
        .isEqualTo(0d);
    assertThat(dr.extend(2))
        .as("extended max")
        .extracting(DoubleRange::max)
        .isEqualTo(20d);
    assertThatThrownBy(() -> dr.extend(-5))
        .as("negative r gives exception")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void extent() {
    DoubleRange dr = new DoubleRange(-3, -1);
    assertThat(dr.extent()).isEqualTo(2);
  }

  @Test
  void intersectionWith() {
    DoubleRange dr1 = new DoubleRange(-1, 3);
    DoubleRange dr2 = new DoubleRange(2, 5);
    assertThat(dr1.intersectionWith(dr2))
        .as("min of intersection")
        .extracting(DoubleRange::min)
        .isEqualTo(2.0);
    assertThat(dr1.intersectionWith(dr2))
        .as("max of intersection")
        .extracting(DoubleRange::max)
        .isEqualTo(3.0);
    assertThat(dr2.intersectionWith(dr1))
        .as("commutativity of intersection")
        .isEqualTo(dr1.intersectionWith(dr2));
  }

  @Test
  void normalize() {
    DoubleRange dr = new DoubleRange(8, 12);
    assertThat(dr.normalize(dr.center()))
        .as("normalize center")
        .isEqualTo(0.5);
    assertThat(dr.normalize(dr.min()))
        .as("normalize min")
        .isEqualTo(0d);
    assertThat(dr.normalize(dr.max()))
        .as("normalize max")
        .isEqualTo(1d);
    assertThat(dr.normalize(dr.min() - 1))
        .as("normalize <min")
        .isEqualTo(0d);
    assertThat(dr.normalize(dr.max() + 1))
        .as("normalize >max")
        .isEqualTo(1d);
  }

  @Test
  void overlaps() {
    DoubleRange dr1 = new DoubleRange(5, 7);
    DoubleRange dr2 = new DoubleRange(6, 100);
    DoubleRange dr3 = new DoubleRange(1, 2);
    assertThat(dr1.overlaps(dr2)).isTrue();
    assertThat(dr1.overlaps(dr3)).isFalse();
    assertThat(dr1.overlaps(dr2))
        .as("commutativity of overlaps")
        .isEqualTo(dr2.overlaps(dr1));
    assertThat(dr1.overlaps(dr3))
        .as("commutativity of overlaps")
        .isEqualTo(dr3.overlaps(dr1));
  }

  @Test
  void points() {
    assertThat(new DoubleRange(2, 4).points(4))
        .as("4 points in 2-wide range")
        .containsExactly(2d, 2.5d, 3d, 3.5d, 4d);
  }

  @Test
  void unionWith() {
    DoubleRange dr1 = new DoubleRange(-1, 3);
    DoubleRange dr2 = new DoubleRange(2, 5);
    assertThat(dr1.unionWith(dr2))
        .as("min of union")
        .extracting(DoubleRange::min)
        .isEqualTo(-1.0);
    assertThat(dr1.unionWith(dr2))
        .as("max of union")
        .extracting(DoubleRange::max)
        .isEqualTo(5.0);
    assertThat(dr2.unionWith(dr1))
        .as("commutativity of union")
        .isEqualTo(dr1.unionWith(dr2));
  }

}