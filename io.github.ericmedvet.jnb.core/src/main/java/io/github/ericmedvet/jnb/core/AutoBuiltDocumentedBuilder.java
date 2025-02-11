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

import io.github.ericmedvet.jnb.core.Param.Injection;
import io.github.ericmedvet.jnb.core.parsing.StringParser;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record AutoBuiltDocumentedBuilder<T>(
    String name, java.lang.reflect.Type builtType, List<ParamInfo> params, Executable origin, Builder<T> builder
) implements DocumentedBuilder<T> {

  private static Object buildDefaultValue(ParamMap.Type type, Class<?> clazz, Param pa) {
    if (type.equals(ParamMap.Type.INT) && pa.dI() != Integer.MIN_VALUE) {
      return pa.dI();
    }
    if (type.equals(ParamMap.Type.DOUBLE) && !Double.isNaN(pa.dD())) {
      return pa.dD();
    }
    if (type.equals(ParamMap.Type.BOOLEAN)) {
      return pa.dB();
    }
    if (type.equals(ParamMap.Type.STRING) && !pa.dS().equals(Param.UNDEFAULTED_STRING)) {
      return pa.dS();
    }
    if (type.equals(ParamMap.Type.ENUM) && !pa.dS().equals(Param.UNDEFAULTED_STRING)) {
      //noinspection rawtypes,unchecked
      return Enum.valueOf((Class) clazz, pa.dS().toUpperCase());
    }
    if (type.equals(ParamMap.Type.NAMED_PARAM_MAP) && !pa.dNPM().equals(Param.UNDEFAULTED_STRING)) {
      return StringParser.parse(pa.dNPM());
    }
    if (type.equals(ParamMap.Type.INTS)) {
      return Arrays.stream(pa.dIs()).boxed().toList();
    }
    if (type.equals(ParamMap.Type.DOUBLES)) {
      return Arrays.stream(pa.dDs()).boxed().toList();
    }
    if (type.equals(ParamMap.Type.BOOLEANS)) {
      List<Boolean> booleans = new ArrayList<>();
      for (boolean b : pa.dBs()) {
        booleans.add(b);
      }
      return Collections.unmodifiableList(booleans);
    }
    if (type.equals(ParamMap.Type.STRINGS)) {
      return List.of(pa.dSs());
    }
    if (type.equals(ParamMap.Type.ENUMS)) {
      //noinspection unchecked,rawtypes
      return Arrays.stream(pa.dSs())
          .map(s -> Enum.valueOf((Class) clazz, s.toUpperCase()))
          .toList();
    }
    if (type.equals(ParamMap.Type.NAMED_PARAM_MAPS)) {
      return Arrays.stream(pa.dNPMs()).map(StringParser::parse).toList();
    }
    return null;
  }

  @SuppressWarnings("unused")
  private static <E extends Enum<E>> Object buildOrDefaultParam(ParamInfo pi, ParamMap m, NamedBuilder<Object> nb) {
    @SuppressWarnings({"rawtypes", "unchecked"}) Object value = m.value(pi.name(), pi.type(), (Class) pi.enumClass());
    if (value != null) {
      return value;
    }
    if (m.names().contains(pi.name())) {
      throw new IllegalArgumentException(
          "Wrong type for param \"%s\": \"%s\" is not %s"
              .formatted(pi.name(), m.value(pi.name()), pi.type().name())
      );
    }
    if (pi.defaultValue() != null) {
      return pi.defaultValue();
    }
    if (pi.type().equals(ParamMap.Type.STRING) && pi.interpolationString() != null) {
      if (m instanceof NamedParamMap npm) {
        return new Interpolator(pi.interpolationString()).interpolate(nb.fillWithDefaults(npm));
      }
      return new Interpolator(pi.interpolationString()).interpolate(m);
    }
    throw new IllegalArgumentException("Unvalued undefaulted parameter \"%s\"".formatted(pi.name()));
  }

  @SuppressWarnings("unchecked")
  private static Object buildParam(ParamInfo pi, ParamMap m, Parameter ap, NamedBuilder<Object> nb) {
    return switch (pi.type()) {
      case NAMED_PARAM_MAP -> processNPM((NamedParamMap) buildOrDefaultParam(pi, m, nb), ap, nb, 0);
      case NAMED_PARAM_MAPS -> processNPMs((List<NamedParamMap>) buildOrDefaultParam(pi, m, nb), ap, nb);
      default -> buildOrDefaultParam(pi, m, nb);
    };
  }

  private static List<Object> processNPMs(List<NamedParamMap> npms, Parameter ap, NamedBuilder<Object> nb) {
    return IntStream.range(0, npms.size())
        .mapToObj(i -> processNPM(npms.get(i), ap, nb, i))
        .toList();
  }

  public static List<DocumentedBuilder<Object>> from(Executable executable, Alias[] aliases) {
    Logger l = Logger.getLogger(AutoBuiltDocumentedBuilder.class.getName());
    // check annotations
    BuilderMethod builderMethodAnnotation = executable.getAnnotation(BuilderMethod.class);
    boolean isCacheable = executable.getAnnotation(Cacheable.class) != null;
    // check public and static or constructor
    if (!Modifier.isPublic(executable.getModifiers())) {
      return List.of();
    }
    if (!Modifier.isStatic(executable.getModifiers()) && executable instanceof Method) {
      return List.of();
    }
    // get name
    String name;
    java.lang.reflect.Type buildType;
    if (executable instanceof Method method) {
      buildType = method.getGenericReturnType();
      name = method.getName();
    } else {
      buildType = ((Constructor<?>) executable).getDeclaringClass();
      name = toLowerCamelCase(
          ((Constructor<?>) executable).getDeclaringClass().getSimpleName()
      );
    }
    if (builderMethodAnnotation != null && !builderMethodAnnotation.value().isEmpty()) {
      name = builderMethodAnnotation.value();
    }
    try {
      // check if has NamedBuilder parameter at the beginning
      boolean hasNamedBuilder = executable.getParameters().length > 0 && executable.getParameters()[0].getType()
          .equals(NamedBuilder.class);
      // find parameters
      List<ParamInfo> paramInfos = Arrays.stream(executable.getParameters())
          .map(AutoBuiltDocumentedBuilder::from)
          .filter(Objects::nonNull)
          .toList();
      if (paramInfos.size() != executable.getParameters().length - (hasNamedBuilder ? 1 : 0)) {
        throw new BuilderException(
            "Cannot build builder \"%s\": %d on %d params are not valid"
                .formatted(
                    name,
                    executable.getParameters().length - (hasNamedBuilder ? 1 : 0) - paramInfos.size(),
                    executable.getParameters().length - (hasNamedBuilder ? 1 : 0)
                )
        );
      }
      // wrap and return
      String finalName = name;
      Builder<Object> builder = (ParamMap map, NamedBuilder<?> namedBuilder, int index) -> {
        Object[] params = new Object[paramInfos.size() + (hasNamedBuilder ? 1 : 0)];
        if (hasNamedBuilder) {
          params[0] = namedBuilder;
        }
        for (int j = 0; j < paramInfos.size(); j++) {
          int k = j + (hasNamedBuilder ? 1 : 0);
          if (paramInfos.get(j).injection().equals(Injection.MAP)) {
            params[k] = map;
          } else if (paramInfos.get(j).injection().equals(Injection.MAP_WITH_DEFAULTS)) {
            if (map instanceof NamedParamMap npm) {
              params[k] = namedBuilder.fillWithDefaults(npm);
            }
          } else if (paramInfos.get(j).injection().equals(Injection.BUILDER)) {
            params[k] = namedBuilder;
          } else if (paramInfos.get(j).injection().equals(Injection.INDEX)) {
            params[k] = index;
          } else {
            try {
              //noinspection unchecked
              params[k] = buildParam(
                  paramInfos.get(j),
                  map,
                  executable.getParameters()[k],
                  (NamedBuilder<Object>) namedBuilder
              );
            } catch (RuntimeException e) {
              throw new BuilderException(
                  "Cannot build param \"%s\" for \"%s\""
                      .formatted(paramInfos.get(j).name(), finalName),
                  e
              );
            }
          }
        }
        // check exceeding params
        Set<String> exceedingParamNames = new TreeSet<>(map.names());
        paramInfos.stream()
            .filter(pi -> pi.injection().equals(Injection.NONE))
            .map(ParamInfo::name)
            .toList()
            .forEach(exceedingParamNames::remove);
        if (!exceedingParamNames.isEmpty()) {
          l.warning(
              String.format(
                  "Exceeding parameters while building %s: %s",
                  finalName,
                  exceedingParamNames
              )
          );
        }
        try {
          if (executable instanceof Method method) {
            return method.invoke(null, params);
          }
          return ((Constructor<?>) executable).newInstance(params);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
                 IllegalArgumentException e) {
          throw new BuilderException("Cannot build \"%s\"".formatted(finalName), e);
        }
      };
      AutoBuiltDocumentedBuilder<Object> mainBuilder = new AutoBuiltDocumentedBuilder<>(
          finalName,
          buildType,
          paramInfos,
          executable,
          isCacheable ? builder.cached() : builder
      );
      Map<String, DocumentedBuilder<Object>> builders = new TreeMap<>();
      builders.put(mainBuilder.name(), mainBuilder);
      for (Alias alias : aliases) {
        String aliasName = fromAlias(alias, null).mapName();
        DocumentedBuilder<Object> toAliasBuilder = builders.get(aliasName);
        if (toAliasBuilder == null) {
          throw new BuilderException(
              "Cannot build alias \"%s\" for builder \"%s\": known builders are %s"
                  .formatted(
                      aliasName,
                      mainBuilder.name,
                      builders.keySet()
                          .stream()
                          .map("\"%s\""::formatted)
                          .collect(Collectors.joining(", "))
                  )
          );
        }
        builders.put(alias.name(), toAliasBuilder.alias(alias.name(), alias));
      }
      return builders.values().stream().toList();
    } catch (Exception ex) {
      throw new BuilderException("Cannot build builder for \"%s\"".formatted(name), ex);
    }
  }

  private static ParamInfo from(Parameter parameter) {
    Param paramAnnotation = parameter.getAnnotation(Param.class);
    if (paramAnnotation == null) {
      return null;
    }
    if (paramAnnotation.injection().equals(Param.Injection.MAP) && !parameter.getType().equals(ParamMap.class)) {
      return null;
    }
    if (paramAnnotation.injection().equals(Param.Injection.BUILDER) && !parameter.getType()
        .equals(NamedBuilder.class)) {
      return null;
    }
    if (paramAnnotation.injection().equals(Param.Injection.INDEX) && !parameter.getType().equals(Integer.TYPE)) {
      return null;
    }
    String name = paramAnnotation.value();
    try {
      if (parameter.getType().equals(Integer.class) || parameter.getType().equals(Integer.TYPE)) {
        return new ParamInfo(
            ParamMap.Type.INT,
            null,
            name,
            buildDefaultValue(ParamMap.Type.INT, Integer.class, paramAnnotation),
            null,
            paramAnnotation.injection(),
            parameter.getParameterizedType()
        );
      }
      if (parameter.getType().equals(Double.class) || parameter.getType().equals(Double.TYPE)) {
        return new ParamInfo(
            ParamMap.Type.DOUBLE,
            null,
            name,
            buildDefaultValue(ParamMap.Type.DOUBLE, Double.class, paramAnnotation),
            null,
            paramAnnotation.injection(),
            parameter.getParameterizedType()
        );
      }
      if (parameter.getType().equals(String.class)) {
        return new ParamInfo(
            ParamMap.Type.STRING,
            null,
            name,
            buildDefaultValue(ParamMap.Type.STRING, String.class, paramAnnotation),
            paramAnnotation.iS().equals(Param.UNDEFAULTED_STRING) ? null : paramAnnotation.iS(),
            paramAnnotation.injection(),
            parameter.getParameterizedType()
        );
      }
      if (parameter.getType().equals(Boolean.class) || parameter.getType().equals(Boolean.TYPE)) {
        return new ParamInfo(
            ParamMap.Type.BOOLEAN,
            null,
            name,
            buildDefaultValue(ParamMap.Type.BOOLEAN, Boolean.class, paramAnnotation),
            null,
            paramAnnotation.injection(),
            parameter.getParameterizedType()
        );
      }
      if (parameter.getType().isEnum()) {
        return new ParamInfo(
            ParamMap.Type.ENUM,
            parameter.getType(),
            name,
            buildDefaultValue(ParamMap.Type.ENUM, parameter.getType(), paramAnnotation),
            null,
            paramAnnotation.injection(),
            parameter.getParameterizedType()
        );
      }
      if (parameter.getType().equals(List.class) && parameter
          .getParameterizedType() instanceof ParameterizedType parameterizedType) {
        if (parameterizedType.getActualTypeArguments()[0].equals(Integer.class)) {
          return new ParamInfo(
              ParamMap.Type.INTS,
              null,
              name,
              buildDefaultValue(ParamMap.Type.INTS, Integer.class, paramAnnotation),
              null,
              paramAnnotation.injection(),
              parameter.getParameterizedType()
          );
        }
      }
      if (parameter.getType().equals(List.class) && parameter
          .getParameterizedType() instanceof ParameterizedType parameterizedType) {
        if (parameterizedType.getActualTypeArguments()[0].equals(Double.class)) {
          return new ParamInfo(
              ParamMap.Type.DOUBLES,
              null,
              name,
              buildDefaultValue(ParamMap.Type.DOUBLES, Double.class, paramAnnotation),
              null,
              paramAnnotation.injection(),
              parameter.getParameterizedType()
          );
        }
      }
      if (parameter.getType().equals(List.class) && parameter
          .getParameterizedType() instanceof ParameterizedType parameterizedType) {
        if (parameterizedType.getActualTypeArguments()[0].equals(String.class)) {
          return new ParamInfo(
              ParamMap.Type.STRINGS,
              null,
              name,
              buildDefaultValue(ParamMap.Type.STRINGS, String.class, paramAnnotation),
              null,
              paramAnnotation.injection(),
              parameter.getParameterizedType()
          );
        }
      }
      if (parameter.getType().equals(List.class) && parameter
          .getParameterizedType() instanceof ParameterizedType parameterizedType) {
        if (parameterizedType.getActualTypeArguments()[0].equals(Boolean.class)) {
          return new ParamInfo(
              ParamMap.Type.BOOLEANS,
              null,
              name,
              buildDefaultValue(ParamMap.Type.BOOLEANS, Boolean.class, paramAnnotation),
              null,
              paramAnnotation.injection(),
              parameter.getParameterizedType()
          );
        }
      }
      if (parameter.getType().equals(List.class) && parameter
          .getParameterizedType() instanceof ParameterizedType parameterizedType) {
        Class<?> clazz = Objects.class;
        if (!parameterizedType.getActualTypeArguments()[0].getTypeName().contains("<")) {
          try {
            clazz = Class.forName(parameterizedType.getActualTypeArguments()[0].getTypeName());
          } catch (ClassNotFoundException e) {
            // ignore
          }
        }
        if (clazz.isEnum()) {
          return new ParamInfo(
              ParamMap.Type.ENUMS,
              clazz,
              name,
              buildDefaultValue(ParamMap.Type.ENUMS, clazz, paramAnnotation),
              null,
              paramAnnotation.injection(),
              parameter.getParameterizedType()
          );
        }
      }
      if (parameter.getType().equals(List.class) && parameter
          .getParameterizedType() instanceof ParameterizedType parameterizedType) {
        if (parameterizedType.getActualTypeArguments()[0].equals(NamedParamMap.class)) {
          return new ParamInfo(
              ParamMap.Type.NAMED_PARAM_MAPS,
              null,
              name,
              buildDefaultValue(ParamMap.Type.NAMED_PARAM_MAPS, NamedParamMap.class, paramAnnotation),
              null,
              paramAnnotation.injection(),
              parameter.getParameterizedType()
          );
        }
      }
      if (parameter.getType().equals(List.class) && parameter.getParameterizedType() instanceof ParameterizedType) {
        return new ParamInfo(
            ParamMap.Type.NAMED_PARAM_MAPS,
            null,
            name,
            buildDefaultValue(ParamMap.Type.NAMED_PARAM_MAPS, Object.class, paramAnnotation),
            null,
            paramAnnotation.injection(),
            parameter.getParameterizedType()
        );
      }
      return new ParamInfo(
          ParamMap.Type.NAMED_PARAM_MAP,
          null,
          name,
          buildDefaultValue(ParamMap.Type.NAMED_PARAM_MAP, Object.class, paramAnnotation),
          null,
          paramAnnotation.injection(),
          parameter.getParameterizedType()
      );
    } catch (Exception ex) {
      throw new BuilderException("Cannot build param info for \"%s\"".formatted(name), ex);
    }
  }

  private static Object processNPM(NamedParamMap npm, Parameter actualParameter, NamedBuilder<Object> nb, int index) {
    if (actualParameter.getType().equals(NamedParamMap.class)) {
      return npm;
    }
    return nb.build(npm, null, index);
  }

  private static String toLowerCamelCase(String s) {
    return s.substring(0, 1).toLowerCase() + s.substring(1);
  }

  static NamedParamMap fromAlias(Alias alias, ParamMap map) {
    BiFunction<String, ParamMap.Type, String> quoter = (s, t) -> t.equals(ParamMap.Type.STRING) ? "\"%s\"".formatted(
        s
    ) : s;
    String consts = Arrays.stream(alias.passThroughParams())
        .map(
            p -> StringParser.CONST_NAME_PREFIX + "%s = %s"
                .formatted(
                    p.name(),
                    quoter.apply(
                        map == null ? (p.value().isEmpty() ? "null" : p.value()) : (map.value(p.name()) == null ? p
                            .value() : map.value(p.name())
                                .toString()),
                        p.type()
                    )
                )
        )
        .collect(Collectors.joining("\n"));
    consts = consts + "\n" + alias.value();
    return StringParser.parse(consts);
  }

  @Override
  public T build(ParamMap map, NamedBuilder<?> namedBuilder, int index) throws BuilderException {
    return builder.build(map, namedBuilder, index);
  }

  @Override
  public String toString() {
    return "(" + params().stream().map(ParamInfo::toString).collect(Collectors.joining("; ")) + ") -> " + builtType();
  }
}
