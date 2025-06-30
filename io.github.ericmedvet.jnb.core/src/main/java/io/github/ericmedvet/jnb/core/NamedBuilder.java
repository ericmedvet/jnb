/*-
 * ========================LICENSE_START=================================
 * jnb-core
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
package io.github.ericmedvet.jnb.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassMemberInfo;
import io.github.classgraph.ScanResult;
import io.github.ericmedvet.jnb.core.MapNamedParamMap.TypedName;
import io.github.ericmedvet.jnb.core.ParamMap.Type;
import io.github.ericmedvet.jnb.core.parsing.StringParser;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A factory that can build objects of type `X` (or a subtype) from a `NamedParamMap`. Internally,
/// it holds a collection of `Builder` instances, each associated with a unique name. When `build`
/// is called, it looks up the appropriate `Builder` based on the `NamedParamMap` name and uses it
/// to construct the object.
///
/// A `NamedBuilder` is typically obtained from existing utility classes (through
/// [NamedBuilder#fromUtilityClass(Class)]), normal classes (through
/// [NamedBuilder#fromClass(Class)]), or from both, automatically discovered in packages in the
/// current classpath (through [NamedBuilder#fromDiscovery(String...)]). Such classes need to be
/// annotated with the proper annotations ([Discoverable], [BuilderMethod], [Alias], [Param]).
///
/// @param <X> the type of objects this builder can produce
public class NamedBuilder<X> {

  /// The character used to separate parts of names in a hierarchical structure.
  public static final char NAME_SEPARATOR = '.';
  /// The character used to separate prefixes synonyms, used by the [Discoverable] annotation.
  public static final char PREFIX_SEPARATOR = '|';
  private static final Logger L = Logger.getLogger(NamedBuilder.class.getName());
  private static final NamedBuilder<Object> EMPTY = new NamedBuilder<>(Map.of());
  private final Map<String, Builder<? extends X>> builders;

  /// Constructs a `NamedBuilder` with the given map of builders.
  ///
  /// @param builders a map where keys are builder names and values are `Builder` instances
  private NamedBuilder(Map<String, Builder<? extends X>> builders) {
    this.builders = new TreeMap<>(builders);
  }

  /// Returns an empty `NamedBuilder` that cannot build any object.
  ///
  /// @return an empty `NamedBuilder`
  public static NamedBuilder<Object> empty() {
    return EMPTY;
  }

  /// Returns a `NamedBuilder` with builders obtained by discovery from classes annotated with
  /// `@Discoverable` within the specified packages (or everywhere if `packageNames` is empty).
  /// Internally, this method work by invoking [NamedBuilder#fromClass(Class)] or
  /// [NamedBuilder#fromUtilityClass(Class)] depending on the case.
  ///
  /// @param packageNames an array of package names to scan for discoverable classes
  /// @return a `NamedBuilder` populated with discovered builders
  public static NamedBuilder<Object> fromDiscovery(String... packageNames) {
    NamedBuilder<Object> nb = NamedBuilder.empty();
    try (ScanResult scanResult = new ClassGraph().enableAllInfo()
        .acceptPackages(packageNames)
        .scan()) {
      for (ClassInfo classInfo : scanResult.getAllClasses()
          .filter(classInfo -> classInfo.hasAnnotation(Discoverable.class))) {
        String[] prefixes = (String[]) classInfo
            .getAnnotationInfo(Discoverable.class)
            .getParameterValues()
            .getValue("prefixes");
        String prefixTemplate = (String) classInfo
            .getAnnotationInfo(Discoverable.class)
            .getParameterValues()
            .getValue("prefixTemplate");
        if (prefixes.length > 0 && !prefixTemplate.isEmpty()) {
          L.warning(
              "Both prefixes and prefixTemplate are set for discoverable class %s: using prefixes %s"
                  .formatted(classInfo.getName(), Arrays.toString(prefixes))
          );
        } else if (prefixes.length == 0 && !prefixTemplate.isEmpty()) {
          List<List<String>> tokens = Arrays.stream(
              prefixTemplate.split(Pattern.quote("" + NAME_SEPARATOR))
          )
              .map(
                  t -> Arrays.stream(t.split(Pattern.quote("" + PREFIX_SEPARATOR)))
                      .toList()
              )
              .toList();
          prefixes = flatTokens(tokens).stream()
              .map(l -> String.join("" + NAME_SEPARATOR, l))
              .toArray(String[]::new);
        }
        if (classInfo.getDeclaredConstructorInfo().stream().noneMatch(ClassMemberInfo::isPublic)) {
          nb = nb.and(
              Arrays.stream(prefixes).toList(),
              NamedBuilder.fromUtilityClass(classInfo.loadClass())
          );
        } else {
          nb = nb.and(
              Arrays.stream(prefixes).toList(),
              NamedBuilder.fromClass(classInfo.loadClass())
          );
        }
      }
    }
    return nb;
  }

  private static List<List<String>> flatTokens(List<List<String>> tokens) {
    if (tokens.size() == 1) {
      return tokens.getFirst().stream().map(List::of).toList();
    }
    return tokens.getFirst()
        .stream()
        .map(
            t -> flatTokens(tokens.subList(1, tokens.size())).stream()
                .map(l -> Stream.concat(Stream.of(t), l.stream()).toList())
                .toList()
        )
        .flatMap(Collection::stream)
        .toList();
  }

  /// Creates a `NamedBuilder` from a given class. It expects the class to have exactly one public
  /// constructor, or exactly one public constructor annotated with `@BuilderMethod`. In that
  /// constructor, parameters (whose type is different of `NamedParameter`, which can appear at most
  /// once, as type) have to be annotated with [Param]. Internally, it invokes
  /// [AutoBuiltDocumentedBuilder#from(java.lang.reflect.Executable, Alias\[\])].
  ///
  /// @param clazz the class from which to create the builder
  /// @param <C>   the type of the class
  /// @return a `NamedBuilder` capable of building instances of `C`, whose name is the simple name
  ///  of the class in lower camel case
  @SuppressWarnings({"unchecked", "unused"})
  public static <C> NamedBuilder<C> fromClass(Class<? extends C> clazz) {
    List<Constructor<?>> constructors = Arrays.stream(clazz.getConstructors()).toList();
    if (constructors.size() > 1) {
      constructors = constructors.stream()
          .filter(constructor -> constructor.getAnnotation(BuilderMethod.class) != null)
          .toList();
    }
    if (constructors.size() != 1) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot build named builder from class %s that has %d!=1 constructors",
              clazz.getSimpleName(),
              clazz.getConstructors().length
          )
      );
    }
    return (NamedBuilder<C>) (new NamedBuilder<>(
        AutoBuiltDocumentedBuilder.from(
            constructors.getFirst(),
            clazz.getAnnotationsByType(Alias.class)
        )
            .stream()
            .collect(Collectors.toMap(DocumentedBuilder::name, b -> b))
    ));
  }

  /// Creates a `NamedBuilder` from a given utility class. It discovers public static methods in the
  /// class and attempts to create builders from them: each public static method in the class will
  /// have a builder whose name is the one of the method.
  ///
  /// @param clazz the utility class from which to create the builder
  /// @return a `NamedBuilder` capable of building objects using the utility methods
  @SuppressWarnings("unused")
  public static NamedBuilder<Object> fromUtilityClass(Class<?> clazz) {
    return new NamedBuilder<>(
        Arrays.stream(clazz.getMethods())
            .map(m -> AutoBuiltDocumentedBuilder.from(m, m.getAnnotationsByType(Alias.class)))
            .flatMap(List::stream)
            .collect(Collectors.toMap(DocumentedBuilder::name, b -> b))
    );
  }

  /// Returns a pretty string representation of the `NamedBuilder`, listing all registered
  /// builders.
  ///
  /// @param namedBuilder the `NamedBuilder` to represent
  /// @param newLine      if true, each builder will be on a new line; otherwise, they will be
  ///                     separated by "; "
  /// @return a string representation of the `NamedBuilder`
  public static String prettyToString(NamedBuilder<?> namedBuilder, boolean newLine) {
    return namedBuilder.builders.entrySet()
        .stream()
        .map(e -> {
          String s = e.getKey();
          if (e.getValue() instanceof DocumentedBuilder<?> db) {
            s = s + db;
          }
          return s;
        })
        .collect(Collectors.joining(newLine ? "\n" : "; "));
  }

  /// Creates a new `NamedBuilder` by adding all builders from `otherNamedBuilder` under a given
  /// prefix. The returned `NamedBuilder will have all the builders of this `NamedBuilder`, with
  /// their original name, and all the builders of `otherNamedBuilder`, with their name prefixed by
  ///`prefix` followed by [NamedBuilder#NAME_SEPARATOR].
  ///
  /// @param prefix            the prefix to apply to the names of the added builders
  /// @param otherNamedBuilder the `NamedBuilder` whose builders are to be added
  /// @return a new `NamedBuilder` with the combined builders
  public NamedBuilder<X> and(String prefix, NamedBuilder<? extends X> otherNamedBuilder) {
    return and(List.of(prefix), otherNamedBuilder);
  }

  /// Creates a new `NamedBuilder` by adding all builders from `otherNamedBuilder` under a list of
  /// given prefixes. Every builder of `otherNamedBuilder` is added once for every prefix, with a
  /// name which is the original name prefixed by `prefix` followed by
  /// [NamedBuilder#NAME_SEPARATOR].
  ///
  /// @param prefixes          a list of prefixes to apply to the names of the added builders
  /// @param otherNamedBuilder the `NamedBuilder` whose builders are to be added
  /// @return a new `NamedBuilder` with the combined builders
  public NamedBuilder<X> and(List<String> prefixes, NamedBuilder<? extends X> otherNamedBuilder) {
    if (prefixes.isEmpty()) {
      return and(otherNamedBuilder);
    }
    Map<String, Builder<? extends X>> allBuilders = new HashMap<>(builders);
    prefixes.forEach(
        prefix -> otherNamedBuilder.builders.forEach(
            (k, v) -> allBuilders.put(prefix.isEmpty() ? k : (prefix + NAME_SEPARATOR + k), v)
        )
    );
    return new NamedBuilder<>(allBuilders);
  }

  /// Creates a new `NamedBuilder` by adding all builders from `otherNamedBuilder` without any
  /// prefix.
  ///
  /// @param otherNamedBuilder the `NamedBuilder` whose builders are to be added
  /// @return a new `NamedBuilder` with the combined builders
  public NamedBuilder<X> and(NamedBuilder<? extends X> otherNamedBuilder) {
    return and("", otherNamedBuilder);
  }

  /// Builds an object of type `T` (a subtype of `X`) from the provided `NamedParamMap`. If no
  /// builder is found for the map name, `defaultSupplier` is used if provided, otherwise a
  /// `BuilderException` is thrown. The `index` is available when building sequences and is expected
  /// to be set accordingly by the caller.
  ///
  /// @param map             the `NamedParamMap` containing the parameters for building the object
  /// @param defaultSupplier a supplier for a default object, used if no builder is found
  /// @param index           the index of the object to build (different from 0 if the object is a
  ///                        part of sequence)
  /// @param <T>             the specific type of object to build
  /// @return the built object
  /// @throws BuilderException if there is no builder for the map name or the building fails and no
  ///                          default supplier is provided
  @SuppressWarnings("unchecked")
  public <T extends X> T build(NamedParamMap map, Supplier<T> defaultSupplier, int index) throws BuilderException {
    if (map == null) {
      throw new BuilderException("Null input map");
    }
    if (!builders.containsKey(map.mapName())) {
      if (defaultSupplier != null) {
        return defaultSupplier.get();
      }
      throw new BuilderException(
          String.format(
              "No builder for %s: closest matches are %s",
              map.mapName(),
              builders.keySet()
                  .stream()
                  .sorted((Comparator.comparing(s -> distance(s, map.mapName()))))
                  .limit(3)
                  .collect(Collectors.joining(", "))
          )
      );
    }
    try {
      return (T) builders.get(map.mapName()).build(map, this, index);
    } catch (BuilderException e) {
      if (defaultSupplier != null) {
        return defaultSupplier.get();
      }
      throw e;
    }
  }

  /// Builds an object of type `T` (a subtype of `X`) from the provided `mapString`, which is parsed
  /// into a `NamedParamMap` using [StringParser#parse(String)]. If no builder is found for the map
  /// name, `defaultSupplier` is used if provided, otherwise a `BuilderException` is thrown.
  ///
  /// @param mapString       the string representation of the `NamedParamMap`
  /// @param defaultSupplier a supplier for a default object, used if no builder is found
  /// @param <T>             the specific type of object to build
  /// @return the built object
  /// @throws BuilderException if there is no builder for the map name or the building fails and no
  ///                          default supplier is provided
  @SuppressWarnings("unused")
  public <T extends X> T build(String mapString, Supplier<T> defaultSupplier) throws BuilderException {
    return build(StringParser.parse(mapString), defaultSupplier, 0);
  }

  /// Builds an object of type `X` using the provided `NamedParamMap`.
  ///
  /// @param map the `NamedParamMap` containing the parameters for building the object
  /// @return the built object
  /// @throws BuilderException if there is no builder for the map name or the building fails
  public X build(NamedParamMap map) throws BuilderException {
    return build(map, null, 0);
  }

  /// Builds an object of type `T` (a subtype of `X`) from the provided `mapString`, which is parsed
  /// into a `NamedParamMap` using [StringParser#parse(String)].
  ///
  /// @param mapString the string representation of the `NamedParamMap`
  /// @return the built object
  /// @throws BuilderException if there is no builder for the map name or the building fails
  @SuppressWarnings("UnusedReturnValue")
  public X build(String mapString) throws BuilderException {
    return build(StringParser.parse(mapString));
  }

  private double distance(String s1, String s2) {
    return distance(s1.chars().boxed().toList(), s2.chars().boxed().toList());
  }

  private <T> Double distance(List<T> ts1, List<T> ts2) {
    int len0 = ts1.size() + 1;
    int len1 = ts2.size() + 1;
    int[] cost = new int[len0];
    int[] newCost = new int[len0];
    for (int i = 0; i < len0; i++) {
      cost[i] = i;
    }
    for (int j = 1; j < len1; j++) {
      newCost[0] = j;
      for (int i = 1; i < len0; i++) {
        int match = ts1.get(i - 1).equals(ts2.get(j - 1)) ? 0 : 1;
        int cost_replace = cost[i - 1] + match;
        int cost_insert = cost[i] + 1;
        int cost_delete = newCost[i - 1] + 1;
        newCost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
      }
      int[] swap = cost;
      cost = newCost;
      newCost = swap;
    }
    return (double) cost[len0 - 1];
  }

  /// Returns an unmodifiable map of the builders currently registered with this `NamedBuilder`.
  ///
  /// @return an unmodifiable map of builders
  public Map<String, Builder<? extends X>> getBuilders() {
    return Collections.unmodifiableMap(builders);
  }

  /// Returns a string representation of this `NamedBuilder`.
  ///
  /// @return a string representation
  @Override
  public String toString() {
    return prettyToString(this, false);
  }

  /// Fills the provided `NamedParamMap` with default values where parameters are missing, based on
  /// the builder, if any, associated with the map name in this `NamedBuilder`. Note that default values are present in
  /// builders of type [DocumentedBuilder], not necessarily in any builder.
  ///
  /// @param map the `NamedParamMap` to fill with defaults
  /// @return a new `NamedParamMap` with default values applied
  public NamedParamMap fillWithDefaults(NamedParamMap map) {
    if (!builders.containsKey(map.mapName())) {
      return map;
    }
    if (!(builders.get(map.mapName()) instanceof DocumentedBuilder<?> builder)) {
      return map;
    }
    // fill map
    Map<TypedName, Object> values = new HashMap<>();
    for (DocumentedBuilder.ParamInfo p : builder.params()
        .stream()
        .sorted((p1, p2) -> p1.interpolationString() == null ? -1 : (p2.interpolationString() == null ? 1 : 0))
        .toList()) {
      if (p.injection() != Param.Injection.NONE) {
        continue;
      }
      @SuppressWarnings({"unchecked", "rawtypes"}) Object value = map.value(
          p.name(),
          p.type(),
          (Class<Enum>) p.enumClass()
      );
      if (value == null) {
        value = p.defaultValue();
        if (value == null && p.type().equals(Type.STRING) && p.interpolationString() != null) {
          value = Interpolator.interpolate(
              p.interpolationString(),
              new MapNamedParamMap(map.mapName(), values)
          );
        }
      }
      if (value instanceof NamedParamMap npm) {
        value = fillWithDefaults(npm);
      }
      values.put(new TypedName(p.name(), p.type()), value);
    }
    return new MapNamedParamMap(map.mapName(), values);
  }
}
