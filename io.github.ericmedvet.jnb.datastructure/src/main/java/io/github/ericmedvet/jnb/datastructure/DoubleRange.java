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
import java.util.List;
import java.util.stream.DoubleStream;

public record DoubleRange(double min, double max) implements Serializable {

  public static final DoubleRange UNIT = new DoubleRange(0, 1);
  public static final DoubleRange SYMMETRIC_UNIT = new DoubleRange(-1, 1);
  public static final DoubleRange UNBOUNDED = new DoubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

  public DoubleRange {
    if (max < min) {
      throw new IllegalArgumentException(
          String.format("Max has to be lower or equal than min; %f is not than %f.", max, min));
    }
  }

  public static DoubleRange largest(List<DoubleRange> ranges) {
    return ranges.stream().reduce(DoubleRange::largest).orElseThrow();
  }

  public static DoubleRange smallest(List<DoubleRange> ranges) {
    return ranges.stream().reduce(DoubleRange::smallest).orElseThrow();
  }

  public double clip(double value) {
    return Math.min(Math.max(value, min), max);
  }

  public boolean contains(double d) {
    return min <= d && d <= max;
  }

  public boolean contains(DoubleRange other) {
    return contains(other.min) && contains(other.max);
  }

  public DoubleRange delta(double v) {
    return new DoubleRange(min + v, max + v);
  }

  public double denormalize(double value) {
    return clip(value * extent() + min());
  }

  public double extent() {
    return max - min;
  }

  public DoubleRange largest(DoubleRange other) {
    return new DoubleRange(Math.min(min, other.min), Math.max(max, other.max));
  }

  public double normalize(double value) {
    return (clip(value) - min) / (max - min);
  }

  public boolean overlaps(DoubleRange other) {
    if (max < other.min) {
      return false;
    }
    return !(min > other.max);
  }

  public DoubleRange smallest(DoubleRange other) {
    return new DoubleRange(Math.max(min, other.min), Math.min(max, other.max));
  }

  public DoubleStream points(int n) {
    double step = extent() / (double) n;
    return DoubleStream.iterate(min, v -> v <= max, v -> v + step);
  }

  public DoubleRange extend(double r) {
    return new DoubleRange(center() - extent() / 2d * r, center() + extent() / 2d * r);
  }

  public double center() {
    if (equals(UNBOUNDED)) {
      return 0d;
    }
    return (min + max) / 2d;
  }
}
