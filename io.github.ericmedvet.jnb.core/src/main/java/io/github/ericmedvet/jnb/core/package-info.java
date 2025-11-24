/*-
 * ========================LICENSE_START=================================
 * jnb-core
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
/// Provides interfaces, classes, annotations, and other Java artifacts for defining and using named builders.
///
/// # Intended usage
/// Very in brief, the intended usage is the one represented in the following two snippets, which are mostly
/// self-explanatory.
///
/// <!-- // @formatter:off -->
/// {@snippet lang="java":
/// public record Office(
///     @Param("roomNubers") List<Integer> roomNumbers,
///     @Param("head") Person head,
///     @Param("staff") Lit<Person> staff
/// ) {}
/// }
/// <!-- // @formatter:on -->
///
/// <!-- // @formatter:off -->
/// {@snippet lang="java":
/// public record Person(
///     @Param("name") String name,
///     @Param(value = "age", dI = 45) int age
/// ) {}
/// }
/// <!-- // @formatter:on -->
///
/// <!-- // @formatter:off -->
/// {@snippet lang="java":
/// public class Functions {
///   private Functions() {}
///
///   @Cacheable
///   public static Function<String, String shortener(
///       @Param(value = i", dS = ".") String suffix
///   ) {
///     return s -> s.charAt(0) + suffix;
///   }
/// }
/// }
/// <!-- // @formatter:on -->
///
/// Here, three classes (`Office`, `Person`, and `Functions`) are annotated for being registered to a
/// [io.github.ericmedvet.jnb.core.NamedBuilder] (see below).
/// Two of them have constructors with parameters annotated with the [io.github.ericmedvet.jnb.core.Param]
/// annotation; one (`Functions`) provides a static method with parameters annotated with the same annotation.
/// Through these parameters, the actual method/constructor parameters are mapped by the `NamedBuilder` to parameters
/// that can be mentioned when describing an instance of the corresponding object to be built.
/// Annotated parameters may have default values, which can be specified with the `dS`,`dI`, etc. parameter of the
/// annotation (see [io.github.ericmedvet.jnb.core.Param]).
///
/// <!-- // @formatter:off -->
/// {@snippet lang="java":
/// public static void main(String[] args) {
///   NamedBuilder<?> namedBuilder = NamedBuilder.empty()
///       .and(NamedBuilder.fromClass(Office.class))
///       .and(NamedBuilder.fromClass(Person.class))
///       .and("f", NamedBuilder.fromUtilityClass(Functions.class));
///   String description = """
///     office(
///       head = person(name = "Mario Rossi"; age = 43);
///       staff = [
///         person(name = Alice; age = 33);
///         person(name = Bob; age = 25);
///         person(name = Charlie)
///       ];
///       roomNumbers = [202:1:205]
///     )
///   """;
///   Office office = (Office) namedBuilder.build(description);
///   System.out.println(office);
///   System.out.printf("The head's name is: %s%n", office.head().name());
///   @SuppressWarnings("unchecked") Function<String, String> f = (Function<String, String>) namedBuilder.build(
///     "f.shortener()"
///   );
///   System.out.printf("The head's short name is: %s%n", office.head().name());
/// }
/// }
/// <!-- // @formatter:on -->
///
/// Here the three annotated classes are registered to an instance `NamedBuilder`.
/// Then, a string is passed to the instance which, in the
/// [io.github.ericmedvet.jnb.core.NamedBuilder#build(java.lang.String)] method, parses it and processes it to
/// return an instance of the corresponding object, hence playing the role of a builder.
/// Unless otherwise specified, the name (from which the `Named` part of `NamedBuilder`) of the builder to be invoked
/// is inferred from the class or method that has been registered.
///
/// The syntax for the string passed to the `build()` method is defined by a context-free grammar presented in
/// [io.github.ericmedvet.jnb.core.parsing.StringParser].
/// Note that the `build()` method internally first transforms the string to a
/// [io.github.ericmedvet.jnb.core.NamedParamMap], through parsing, then it consumes this map for actually building
/// the object.
/// The second step can be done explicitly by invoking
/// [io.github.ericmedvet.jnb.core.NamedBuilder#build(io.github.ericmedvet.jnb.core.NamedParamMap)].
package io.github.ericmedvet.jnb.core;
