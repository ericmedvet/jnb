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

import org.junit.jupiter.api.Test;

class DoubleRangeTest {

  @Test
  void intersection() {
    DoubleRange dr1 = new DoubleRange(-1, 3);
    DoubleRange dr2 = new DoubleRange(2, 5);
    DoubleRange dr1WithDr2 = dr1.intersectionWith(dr2);
    assertThat(dr1WithDr2.min()).isEqualTo(2);
    assertThat(dr1WithDr2.max()).isEqualTo(3);
    DoubleRange dr2WithDr1 = dr1.intersectionWith(dr2);
    assertThat(dr1WithDr2).isEqualTo(dr2WithDr1);
  }

  @Test
  void union() {
  }

  @Test
  void center() {
  }

  @Test
  void clip() {
  }

  @Test
  void contains() {
  }

  @Test
  void testContains() {
  }

  @Test
  void delta() {
  }

  @Test
  void denormalize() {
  }

  @Test
  void extend() {
  }

  @Test
  void extent() {
  }

  @Test
  void intersectionWith() {
  }

  @Test
  void normalize() {
  }

  @Test
  void overlaps() {
  }

  @Test
  void points() {
  }

  @Test
  void unionWith() {
  }

  @Test
  void min() {
  }

  @Test
  void max() {
  }
}