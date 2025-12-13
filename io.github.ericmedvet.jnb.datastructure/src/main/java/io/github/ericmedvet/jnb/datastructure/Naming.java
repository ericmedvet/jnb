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

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/// This class provides `static` methods for creating named versions of various functional interfaces.
///
/// This class provides static methods to wrap various functional interfaces (like `Consumer`,
/// `Function`, `Predicate`, etc.) with an anonymous class that overrides the `toString()` method to
/// return a given name. This is useful for debugging, logging, or when a human-readable identifier
/// is needed for a functional object.
///
public class Naming {

  private Naming() {
  }

  /// Creates a named `BiConsumer`.
  ///
  /// @param name       the name to assign to the `BiConsumer`
  /// @param biConsumer the `BiConsumer` to be named
  /// @param <I1>       the type of the first input to the consumer
  /// @param <I2>       the type of the second input to the consumer
  /// @return a named `BiConsumer`
  public static <I1, I2> BiConsumer<I1, I2> named(String name, BiConsumer<I1, I2> biConsumer) {
    return new BiConsumer<>() {
      @Override
      public void accept(I1 i1, I2 i2) {
        biConsumer.accept(i1, i2);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `Consumer`.
  ///
  /// @param name     the name to assign to the `Consumer`
  /// @param consumer the `Consumer` to be named
  /// @param <I>      the type of the input to the consumer
  /// @return a named `Consumer`
  public static <I> Consumer<I> named(String name, Consumer<I> consumer) {
    return new Consumer<>() {
      @Override
      public void accept(I i) {
        consumer.accept(i);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `Accumulator`.
  ///
  /// @param name        the name to assign to the `Accumulator`
  /// @param accumulator the `Accumulator` to be named
  /// @param <E>         the type of elements listened by the accumulator
  /// @param <O>         the type of the output produced by the accumulator
  /// @return a named `Accumulator`
  public static <E, O> Accumulator<E, O> named(String name, Accumulator<E, O> accumulator) {
    return new Accumulator<>() {
      @Override
      public O get() {
        return accumulator.get();
      }

      @Override
      public void listen(E e) {
        accumulator.listen(e);
      }

      @Override
      public void done() {
        accumulator.done();
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `Listener`.
  ///
  /// @param name     the name to assign to the `Listener`
  /// @param listener the `Listener` to be named
  /// @param <E>      the type of elements listened by the listener
  /// @return a named `Listener`
  public static <E> Listener<E> named(String name, Listener<E> listener) {
    return new Listener<>() {
      @Override
      public void listen(E e) {
        listener.listen(e);
      }

      @Override
      public void done() {
        listener.done();
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `ListenerFactory`.
  ///
  /// @param name            the name to assign to the `ListenerFactory`
  /// @param listenerFactory the `ListenerFactory` to be named
  /// @param <E>             the type of elements listened by the listeners produced by the factory
  /// @param <K>             the type of the key used to build listeners
  /// @return a named `ListenerFactory`
  public static <E, K> ListenerFactory<E, K> named(
      String name,
      ListenerFactory<E, K> listenerFactory
  ) {
    return new ListenerFactory<>() {
      @Override
      public Listener<E> build(K k) {
        return listenerFactory.build(k);
      }

      @Override
      public void shutdown() {
        listenerFactory.shutdown();
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `AccumulatorFactory`.
  ///
  /// @param name               the name to assign to the `AccumulatorFactory`
  /// @param accumulatorFactory the `AccumulatorFactory` to be named
  /// @param <E>                the type of elements listened by the accumulators produced by the
  ///                           factory
  /// @param <O>                the type of the output produced by the accumulators
  /// @param <K>                the type of the key used to build accumulators
  /// @return a named `AccumulatorFactory`
  public static <E, O, K> AccumulatorFactory<E, O, K> named(
      String name,
      AccumulatorFactory<E, O, K> accumulatorFactory
  ) {
    return new AccumulatorFactory<>() {
      @Override
      public Accumulator<E, O> build(K k) {
        return accumulatorFactory.build(k);
      }

      @Override
      public void shutdown() {
        accumulatorFactory.shutdown();
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `TriConsumer`.
  ///
  /// @param name     the name to assign to the `TriConsumer`
  /// @param triConsumer the `TriConsumer` to be named
  /// @param <I1>     the type of the first input to the consumer
  /// @param <I2>     the type of the second input to the consumer
  /// @param <I3>     the type of the third input to the consumer
  /// @return a named `TriConsumer`
  public static <I1, I2, I3> TriConsumer<I1, I2, I3> named(
      String name,
      TriConsumer<I1, I2, I3> triConsumer
  ) {
    return new TriConsumer<>() {
      @Override
      public void accept(I1 i1, I2 i2, I3 i3) {
        triConsumer.accept(i1, i2, i3);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `DoubleUnaryOperator`.
  ///
  /// @param name     the name to assign to the `DoubleUnaryOperator`
  /// @param operator the `DoubleUnaryOperator` to be named
  /// @return a named `DoubleUnaryOperator`
  public static DoubleUnaryOperator named(String name, DoubleUnaryOperator operator) {
    return new DoubleUnaryOperator() {
      @Override
      public double applyAsDouble(double operand) {
        return operator.applyAsDouble(operand);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `Function`.
  ///
  /// @param name     the name to assign to the `Function`
  /// @param function the `Function` to be named
  /// @param <T>      the type of the input to the function
  /// @param <R>      the type of the result of the function
  /// @return a named `Function`
  public static <T, R> Function<T, R> named(String name, Function<T, R> function) {
    return NamedFunction.from(function, name);
  }

  /// Creates a named `BiFunction`.
  ///
  /// @param name       the name to assign to the `BiFunction`
  /// @param biFunction the `BiFunction` to be named
  /// @param <T>        the type of the first input to the function
  /// @param <U>        the type of the second input to the function
  /// @param <R>        the type of the result of the function
  /// @return a named `BiFunction`
  public static <T, U, R> BiFunction<T, U, R> named(String name, BiFunction<T, U, R> biFunction) {
    return new BiFunction<>() {
      @Override
      public R apply(T t, U u) {
        return biFunction.apply(t, u);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `TriFunction`.
  ///
  /// @param name        the name to assign to the `TriFunction`
  /// @param triFunction the `TriFunction` to be named
  /// @param <I1>        the type of the first input to the function
  /// @param <I2>        the type of the second input to the function
  /// @param <I3>        the type of the third input to the function
  /// @param <O>         the type of the result of the function
  /// @return a named `TriFunction`
  public static <I1, I2, I3, O> TriFunction<I1, I2, I3, O> named(
      String name,
      TriFunction<I1, I2, I3, O> triFunction
  ) {
    return new TriFunction<>() {
      @Override
      public O apply(I1 i1, I2 i2, I3 i3) {
        return triFunction.apply(i1, i2, i3);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `Predicate`.
  ///
  /// @param name      the name to assign to the `Predicate`
  /// @param predicate the `Predicate` to be named
  /// @param <T>       the type of the input to the predicate
  /// @return a named `Predicate`
  public static <T> Predicate<T> named(
      String name,
      Predicate<T> predicate
  ) {
    return new Predicate<>() {
      @Override
      public boolean test(T t) {
        return predicate.test(t);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  /// Creates a named `Comparator`.
  ///
  /// @param name       the name to assign to the `Comparator`
  /// @param comparator the `Comparator` to be named
  /// @param <C>        the type of objects that may be compared by this comparator
  /// @return a named `Comparator`
  public static <C> Comparator<C> named(String name, Comparator<C> comparator) {
    return new Comparator<>() {
      @Override
      public int compare(C c1, C c2) {
        return comparator.compare(c1, c2);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

}