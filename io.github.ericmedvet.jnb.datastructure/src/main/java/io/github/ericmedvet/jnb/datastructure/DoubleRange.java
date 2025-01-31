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

/// An interval delimited by two bounds, `min` and `max`.
///
/// @param min the lower bound of the interval
/// @param max the upper bound of the interval
public record DoubleRange(double min, double max) implements Serializable {

  /// The interval `$[0,1]$`.
  public static final DoubleRange UNIT = new DoubleRange(0, 1);
  /// The interval `$[-1,1]$`.
  public static final DoubleRange SYMMETRIC_UNIT = new DoubleRange(-1, 1);
  /// The unbound interval `$[-\infty,\infty]$`.
  public static final DoubleRange UNBOUNDED = new DoubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

  /// Default constructor.
  ///
  /// @param min the lower bound of the interval
  /// @param max the upper bound of the interval
  /// @throws IllegalArgumentException if `max` is lower than `min`
  public DoubleRange {
    if (max < min) {
      throw new IllegalArgumentException(
          String.format("Max has to be lower or equal than min; %f is not than %f.", max, min)
      );
    }
  }

  /// Builds an interval which has the lowest lower bound across all the input intervals and the highest upper
  /// bound across all the input intervals.
  /// The returned interval is the smallest interval that contains all the input intervals.
  ///
  /// @param ranges the input intervals
  /// @return the smallest interval that contains all the input intervals
  public static DoubleRange largest(List<DoubleRange> ranges) {
    return ranges.stream().reduce(DoubleRange::largest).orElseThrow();
  }

  /// Builds an interval which has the highest lower bound across all the input intervals and the lowest upper
  /// bound across all the input intervals.
  /// The built interval is the intersection of all the input intervals, if not empty.
  ///
  /// @param ranges the input intervals
  /// @return the intersection of all the input intervals
  /// @throws IllegalArgumentException if the intersection of `ranges` is empty
  public static DoubleRange smallest(List<DoubleRange> ranges) {
    return ranges.stream().reduce(DoubleRange::smallest).orElseThrow();
  }

  /// Returns the center of the interval, i.e., (`min`+`max`)/2.
  ///
  /// @return the center of the interval
  public double center() {
    if (equals(UNBOUNDED)) {
      return 0d;
    }
    return (min + max) / 2d;
  }

  /// Clips (i.e., clamps) the given `value` into this interval.
  /// Returns `value` if `value`>=`min` and `value`<=`max`, else `min` if `value`<`min`, else `max`.
  ///
  /// @param value the input value
  /// @return the clipped value
  public double clip(double value) {
    return Math.min(Math.max(value, min), max);
  }

  /// Checks if the given `value` belongs to this interval (assuming both bound included).
  ///
  /// @param value the input value
  /// @return true if `value` belongs the interval, false otherwise
  public boolean contains(double value) {
    return min <= value && value <= max;
  }

  /// Checks if the given `other` interval is contained in this interval.
  /// This holds if both `other` bounds are contained in this interval.
  ///
  /// @param other the other interval
  /// @return true if `other` is contained in this, false otherwise
  public boolean contains(DoubleRange other) {
    return contains(other.min) && contains(other.max);
  }

  /// Returns a new interval which is shifted by the given `offset` with respect to this interval.
  /// If this interval is `$[a,b]$` and `offset` is `$\delta$`, then returns `$[a+\delta,b+\delta]$`.
  ///
  /// @param offset the offset to shift
  /// @return another interval shifted by an `offset`
  public DoubleRange delta(double offset) {
    return new DoubleRange(min + offset, max + offset);
  }

  /// Denormalizes, by shifting and rescaling, the given normalized `value` to this interval.
  /// If this interval is `$[a,b]$` and `value` is `$v \in [0,1]$`, then returns `$a+v(b-a)$`.
  ///
  /// @param value the normalized, i.e., in `$[0,1]$` value
  /// @return the denormalized value
  public double denormalize(double value) {
    return clip(value * extent() + min());
  }

  /// Returns a new interval which is extended by a factor `r` with respect to this interval but with the same center.
  /// If this interval is `$[a,b]$` and `r` is `$r$`, then returns `$[(a+b)/2-r(b-a)/2,(a+b)/2+r(b-a)/2]$`.
  ///
  /// @param r the extent rescaling factor
  /// @return the extended interval
  public DoubleRange extend(double r) {
    return new DoubleRange(center() - extent() / 2d * r, center() + extent() / 2d * r);
  }

  /// Returns the extent of this interval.
  /// If this interval is `$[a,b]$`, then returns `$b-a$`.
  ///
  /// @return the extent
  public double extent() {
    return max - min;
  }

  /// Returns an interval which has the lowest lower bound of this and the `other` interval and the highest upper
  /// bound of this and the `other` interval.
  /// The returned interval is the smallest interval that contains both this and the `other` interval.
  /// If this interval is `$[a,b]$` and `other` is `$[c,d]$`, then returns `$[\min(a,c),\max(b,d)]$`.
  ///
  /// @param other the other interval
  /// @return the smallest interval containing this and `other`
  public DoubleRange largest(DoubleRange other) {
    return new DoubleRange(Math.min(min, other.min), Math.max(max, other.max));
  }

  /// Normalizes, by shifting and rescaling, the given `value` to this interval.
  /// If this interval is `$[a,b]$` and `value` is `$v \in [a,b]$`, then returns `$(v-a)/(b-a)$`.
  ///
  /// @param value the value in this interval to be normalized
  /// @return the normalized value, i.e., a number in `$[0,1]$`
  public double normalize(double value) {
    return (clip(value) - min) / (max - min);
  }

  /// Checks if this interval overlaps the `other` interval.
  ///
  /// @param other the other interval
  /// @return true if this overlaps `other`, false otherwise
  public boolean overlaps(DoubleRange other) {
    if (max < other.min) {
      return false;
    }
    return !(min > other.max);
  }

  /// Returns the `DoubleStream` of the most widespread `n` equispaced values in this interval.
  /// Builds the `DoubleStream` through [DoubleStream#iterate(double, DoublePredicate, DoubleUnaryOperator)] applying
  ///  an increment given by the extent of this interval divided by `n` and starting from the lower bound of this
  /// interval.
  ///
  /// @param n the number of points
  /// @return a stream of `n` equispaced points in this interval
  public DoubleStream points(int n) {
    double step = extent() / (double) n;
    return DoubleStream.iterate(min, v -> v <= max, v -> v + step);
  }

  /// Returns an interval which has the highest lower bound of this and the `other` interval and the lowest upper
  /// bound of this and the `other` interval.
  /// The returned interval is the intersection of this and the `other` interval, if not empty.
  /// If this interval is `$[a,b]$` and `other` is `$[c,d]$`, then returns `$[\max(a,c),\min(b,d)]$`.
  ///
  /// @param other the other interval
  /// @return the intersection of this and `other`
  /// @throws IllegalArgumentException if the intersection of this and `other` is empty
  public DoubleRange smallest(DoubleRange other) {
    if (Math.max(min, other.min) > Math.min(max, other.max)) {
      throw new IllegalArgumentException("Empty intersection");
    }
    return new DoubleRange(Math.max(min, other.min), Math.min(max, other.max));
  }
}
