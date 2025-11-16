/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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

package io.github.ericmedvet.jnb.buildable;

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.AccumulatorFactory;
import io.github.ericmedvet.jnb.datastructure.CSVPrinter;
import io.github.ericmedvet.jnb.datastructure.FormattedFunction;
import io.github.ericmedvet.jnb.datastructure.Listener;
import io.github.ericmedvet.jnb.datastructure.ListenerFactory;
import io.github.ericmedvet.jnb.datastructure.Naming;
import io.github.ericmedvet.jnb.datastructure.TabularPrinter;
import io.github.ericmedvet.jnb.datastructure.Utils;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

@Discoverable(prefixTemplate = "listener|list")
public class Listeners {

  private static final Logger L = Logger.getLogger(Listeners.class.getName());

  private Listeners() {
  }

  @SuppressWarnings("unused")
  public static <E, K> Function<Executor, ListenerFactory<E, K>> console(
      @Param("defaultEFunctions") List<Function<E, ?>> defaultEFunctions,
      @Param(value = "eFunctions") List<Function<E, ?>> eFunctions,
      @Param("defaultKFunctions") List<Function<K, ?>> defaultKFunctions,
      @Param("kFunctions") List<Function<K, ?>> kFunctions,
      @Param(value = "deferred") boolean deferred,
      @Param(value = "onlyLast") boolean onlyLast,
      @Param(value = "eCondition", dNPM = "predicate.always()") Predicate<E> ePredicate,
      @Param(value = "kCondition", dNPM = "predicate.always()") Predicate<K> kPredicate,
      @Param("logExceptions") boolean logExceptions
  ) {
    return executor -> new CustomListenerFactory<>(
        new TabularPrinter<>(
            Utils.concat(defaultEFunctions, eFunctions),
            Utils.concat(defaultKFunctions, kFunctions),
            logExceptions
        ),
        kPredicate,
        ePredicate,
        deferred ? executor : null,
        onlyLast
    );
  }

  @SuppressWarnings("unused")
  public static <E, K> Function<Executor, ListenerFactory<E, K>> csv(
      @Param("defaultEFunctions") List<Function<E, ?>> defaultEFunctions,
      @Param(value = "eFunctions") List<Function<E, ?>> eFunctions,
      @Param("defaultKFunctions") List<Function<K, ?>> defaultKFunctions,
      @Param("kFunctions") List<Function<K, ?>> kFunctions,
      @Param(value = "deferred") boolean deferred,
      @Param(value = "onlyLast") boolean onlyLast,
      @Param(value = "eCondition", dNPM = "predicate.always()") Predicate<E> ePredicate,
      @Param(value = "kCondition", dNPM = "predicate.always()") Predicate<K> kPredicate,
      @Param("path") String path,
      @Param(value = "errorString", dS = "NA") String errorString,
      @Param(value = "intFormat", dS = "%d") String intFormat,
      @Param(value = "doubleFormat", dS = "%.5e") String doubleFormat
  ) {
    return executor -> new CustomListenerFactory<>(
        new CSVPrinter<>(
            Utils.concat(defaultEFunctions, eFunctions),
            Utils.concat(defaultKFunctions, kFunctions),
            path,
            errorString,
            intFormat,
            doubleFormat
        ),
        kPredicate,
        ePredicate,
        deferred ? executor : null,
        onlyLast
    );
  }

  @SuppressWarnings("unused")
  public static <E, O, P, K> Function<Executor, ListenerFactory<E, K>> onDone(
      @Param("of") AccumulatorFactory<E, O, K> accumulatorFactory,
      @Param(value = "preprocessor", dNPM = "f.identity()") Function<? super O, ? extends P> preprocessor,
      @Param(
          value = "consumers", dNPMs = {"ea.consumer.deaf()"}) List<BiConsumer<? super P, K>> consumers,
      @Param(value = "deferred") boolean deferred,
      @Param(value = "eCondition", dNPM = "predicate.always()") Predicate<E> ePredicate,
      @Param(value = "kCondition", dNPM = "predicate.always()") Predicate<K> kPredicate
  ) {
    return executor -> new CustomListenerFactory<>(
        accumulatorFactory.thenOnShutdown(
            Naming.named(
                consumers.toString(),
                (Consumer<O>) (o -> {
                  if (o != null) {
                    P p = preprocessor.apply(o);
                    consumers.forEach(c -> c.accept(p, null));
                  }
                })
            )
        ),
        kPredicate,
        ePredicate,
        deferred ? executor : null,
        false
    );
  }

  @SuppressWarnings("unused")
  public static <E, O, P, K> Function<Executor, ListenerFactory<E, K>> onKDone(
      @Param("of") AccumulatorFactory<E, O, K> accumulatorFactory,
      @Param(value = "preprocessor", dNPM = "f.identity()") Function<? super O, ? extends P> preprocessor,
      @Param(
          value = "consumers", dNPMs = {"ea.consumer.deaf()"}) List<BiConsumer<? super P, K>> consumers,
      @Param(value = "deferred") boolean deferred,
      @Param(value = "eCondition", dNPM = "predicate.always()") Predicate<E> ePredicate,
      @Param(value = "kCondition", dNPM = "predicate.always()") Predicate<K> kPredicate
  ) {
    return executor -> new CustomListenerFactory<>(
        accumulatorFactory.thenOnDone(
            Naming.named(
                consumers.toString(),
                (k, a) -> {
                  P p = preprocessor.apply(a.get());
                  consumers.forEach(c -> c.accept(p, k));
                }
            )
        ),
        kPredicate,
        ePredicate,
        deferred ? executor : null,
        false
    );
  }

  private static <T, R> Function<T, R> reformatToFit(Function<T, R> f, Collection<?> ts) {
    //noinspection unchecked
    return FormattedFunction.from(f)
        .reformattedToFit(ts.stream().map(t -> (T) t).toList());
  }

  private static class CustomListenerFactory<E, K> implements ListenerFactory<E, K> {

    private final ListenerFactory<E, K> innerListenerFactory;

    public CustomListenerFactory(
        ListenerFactory<E, K> innerListenerFactory,
        Predicate<K> kPredicate,
        Predicate<E> ePredicate,
        Executor executor,
        boolean onLast
    ) {
      if (executor != null) {
        innerListenerFactory = innerListenerFactory.deferred(executor);
      }
      if (onLast) {
        innerListenerFactory = innerListenerFactory.onLast();
      }
      this.innerListenerFactory = innerListenerFactory.conditional(kPredicate, ePredicate);
    }

    @Override
    public Listener<E> build(K k) {
      return innerListenerFactory.build(k);
    }

    @Override
    public void shutdown() {
      innerListenerFactory.shutdown();
    }

    @Override
    public String toString() {
      return innerListenerFactory.toString();
    }
  }

}
