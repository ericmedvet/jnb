/*
 * Copyright 2022 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jnb.core;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NamedBuilder<X> {

  protected final static char NAME_SEPARATOR = '.';
  private final static NamedBuilder<Object> EMPTY = new NamedBuilder<>(Map.of());
  private final Map<String, Builder<? extends X>> builders;

  private NamedBuilder(Map<String, Builder<? extends X>> builders) {
    this.builders = new TreeMap<>(builders);
  }

  public static NamedBuilder<Object> empty() {
    return EMPTY;
  }

  @SuppressWarnings({"unchecked", "unused"})
  public static <C> NamedBuilder<C> fromClass(Class<? extends C> c) {
    List<Constructor<?>> constructors = Arrays.stream(c.getConstructors()).toList();
    if (constructors.size() > 1) {
      constructors = constructors.stream()
          .filter(constructor -> constructor.getAnnotation(BuilderMethod.class) != null)
          .toList();
    }
    if (constructors.size() != 1) {
      throw new IllegalArgumentException(String.format(
          "Cannot build named builder from class %s that has %d!=1 constructors",
          c.getSimpleName(),
          c.getConstructors().length
      ));
    }
    DocumentedBuilder<C> builder = (DocumentedBuilder<C>) AutoBuiltDocumentedBuilder.from(constructors.get(0));
    if (builder != null) {
      return new NamedBuilder<>(Map.of(
          builder.name(), builder
      ));
    } else {
      return (NamedBuilder<C>) empty();
    }
  }

  @SuppressWarnings("unused")
  public static NamedBuilder<Object> fromUtilityClass(Class<?> c) {
    return new NamedBuilder<>(Arrays.stream(c.getMethods())
        .map(AutoBuiltDocumentedBuilder::from)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(DocumentedBuilder::name, b -> b)));
  }

  public static String prettyToString(NamedBuilder<?> namedBuilder, boolean newLine) {
    return namedBuilder.builders.entrySet().stream()
        .map(e -> {
          String s = e.getKey();
          if (e.getValue() instanceof DocumentedBuilder<?> db) {
            s = s + db;
          }
          return s;
        })
        .collect(Collectors.joining(newLine ? "\n" : "; "));
  }

  public NamedBuilder<X> and(String prefix, NamedBuilder<? extends X> namedBuilder) {
    return and(List.of(prefix), namedBuilder);
  }

  public NamedBuilder<X> and(List<String> prefixes, NamedBuilder<? extends X> namedBuilder) {
    Map<String, Builder<? extends X>> allBuilders = new HashMap<>(builders);
    prefixes.forEach(
        prefix -> namedBuilder.builders
            .forEach((k, v) -> allBuilders.put(prefix.isEmpty() ? k : (prefix + NAME_SEPARATOR + k), v))
    );
    return new NamedBuilder<>(allBuilders);
  }

  public NamedBuilder<X> and(NamedBuilder<? extends X> namedBuilder) {
    return and("", namedBuilder);
  }

  @SuppressWarnings("unchecked")
  public <T extends X> T build(NamedParamMap map, Supplier<T> defaultSupplier, int index) throws BuilderException {
    if (!builders.containsKey(map.getName())) {
      if (defaultSupplier != null) {
        return defaultSupplier.get();
      }
      throw new BuilderException(String.format(
          "No builder for %s: closest matches are %s",
          map.getName(),
          builders.keySet().stream()
              .sorted((Comparator.comparing(s -> distance(s, map.getName()))))
              .limit(3)
              .collect(Collectors.joining(", "))
      ));
    }
    try {
      return (T) builders.get(map.getName()).build(map, this, index);
    } catch (BuilderException e) {
      if (defaultSupplier != null) {
        return defaultSupplier.get();
      }
      throw new BuilderException(String.format("Cannot build %s: %s", map.getName(), e), e);
    }
  }


  @SuppressWarnings("unused")
  public <T extends X> T build(String mapString, Supplier<T> defaultSupplier) throws BuilderException {
    return build(StringParser.parse(mapString), defaultSupplier, 0);
  }

  public X build(NamedParamMap map) throws BuilderException {
    return build(map, null, 0);
  }

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

  public Map<String, Builder<? extends X>> getBuilders() {
    return Collections.unmodifiableMap(builders);
  }

  @Override
  public String toString() {
    return prettyToString(this, false);
  }

  public NamedParamMap fillWithDefaults(NamedParamMap map) {
    if (!builders.containsKey(map.getName())) {
      return map;
    }
    if (!(builders.get(map.getName()) instanceof DocumentedBuilder<?> builder)) {
      return map;
    }
    //fill map
    SortedMap<String, Double> dMap = new TreeMap<>();
    SortedMap<String, String> sMap = new TreeMap<>();
    SortedMap<String, NamedParamMap> npmMap = new TreeMap<>();
    SortedMap<String, List<Double>> dsMap = new TreeMap<>();
    SortedMap<String, List<String>> ssMap = new TreeMap<>();
    SortedMap<String, List<NamedParamMap>> npmsMap = new TreeMap<>();
    for (DocumentedBuilder.ParamInfo p : builder.params()) {
      if (p.injection() != Param.Injection.NONE) {
        continue;
      }
      switch (p.type()) {
        case INT -> {
          if (map.d(p.name()) != null) {
            dMap.put(p.name(), map.d(p.name()));
          } else {
            if (p.defaultValue() instanceof Integer value) {
              dMap.put(p.name(), value.doubleValue());
            }
          }
        }
        case DOUBLE -> {
          if (map.d(p.name()) != null) {
            dMap.put(p.name(), map.d(p.name()));
          } else {
            if (p.defaultValue() instanceof Double value) {
              dMap.put(p.name(), value);
            }
          }
        }
        case STRING -> {
          if (map.s(p.name()) != null) {
            sMap.put(p.name(), map.s(p.name()));
          } else {
            if (p.defaultValue() instanceof String value) {
              sMap.put(p.name(), value);
            }
          }
        }
        case ENUM, BOOLEAN -> {
          if (map.s(p.name()) != null) {
            sMap.put(p.name(), map.s(p.name()));
          } else {
            if (p.defaultValue() instanceof String value) {
              sMap.put(p.name(), value.toLowerCase());
            }
          }
        }
        case NAMED_PARAM_MAP -> {
          NamedParamMap innerMap = map.npm(p.name());
          if (innerMap != null) {
            innerMap = fillWithDefaults(innerMap);
            npmMap.put(p.name(), fillWithDefaults(innerMap));
          } else {
            if (p.defaultValue() instanceof String value) {
              npmMap.put(p.name(), fillWithDefaults(StringParser.parse(value)));
            }
          }
        }
        case INTS -> //noinspection unchecked
            dsMap.put(
                p.name(),
                map.is(p.name(), (List<Integer>) p.defaultValue())
                    .stream()
                    .mapToDouble(i -> i)
                    .boxed()
                    .toList()
            );
        case DOUBLES -> //noinspection unchecked
            dsMap.put(
                p.name(),
                map.ds(p.name(), (List<Double>) p.defaultValue())
            );
        case STRINGS -> //noinspection unchecked
            ssMap.put(
                p.name(),
                map.ss(p.name(), (List<String>) p.defaultValue())
            );
        case ENUMS, BOOLEANS -> ssMap.put(
            p.name(),
            map.ss(p.name(), ((List<?>) p.defaultValue())
                .stream()
                .map(o -> o.toString().toLowerCase())
                .toList())
        );
        case NAMED_PARAM_MAPS -> //noinspection unchecked
            npmsMap.put(
                p.name(),
                map.npms(p.name(), ((List<NamedParamMap>) p.defaultValue())
                    .stream()
                    .map(this::fillWithDefaults)
                    .toList())
            );
        default -> {
        }
      }
    }
    return new MapNamedParamMap(map.getName(), dMap, sMap, npmMap, dsMap, ssMap, npmsMap);
  }

}
