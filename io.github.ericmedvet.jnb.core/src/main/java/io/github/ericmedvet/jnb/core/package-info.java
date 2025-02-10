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
/// # Intended usage
/// Very in brief, the intended usage is the one represented in [this example](io.github.ericmedvet.jnb
/// .sample/src/main/java/io/github/ericmedvet/jnb/Starter.java), which is mostly self-explanatory:
///
/// <!-- @formatter:off -->
/// ```java
/// public record Office(
///     @Param("roomNumbers") List<Integer> roomNumbers,
///     @Param("head") Person head,
///     @Param("staff") List<Person> staff
/// ) {}
///
/// public record Person(
///     @Param("name") String name,
///     @Param(value = "age", dI = 45) int age
/// ) {}
///
/// @Discoverable(prefixTemplate = "function")
/// public class Functions {
///   private Functions() {}
///   @Cached
///   public static Function<String, String> shortener(
///       @Param(value = "suffix", dS = ".") String suffix
///   ) {
///     return s -> s.charAt(0) + suffix;
///   }
/// }
///
/// public static void main(String[] args) {
///   String description = """
///       office(
///         head = person(name = "Mario Rossi"; age = 43);
///         staff = [
///           person(name = Alice; age = 33);
///           person(name = Bob; age = 25);
///           person(name = Charlie)
///         ];
///         roomNumbers = [202:1:205]
///       )
///       """;
///   NamedBuilder<?> namedBuilder = NamedBuilder.empty()
///       .and(NamedBuilder.fromClass(Office.class))
///       .and(NamedBuilder.fromClass(Person.class))
///       .and(NamedBuilder.fromUtilityClass(Functtions.class));
///   Office office = (Office) namedBuilder.build(description);
///   System.out.println(office);
///   System.out.printf("The head's name is: %s%n", office.head().name());
///   @SuppressWarnings("unchecked") Function<String, String> f = (Function<String, String>) namedBuilder.build("f.shortener()");
///   System.out.printf("The head's short name is: %s%n", office.head().name());
/// }
/// ```
/// <!-- @formatter:on -->
///
/// # `NamedBuilder`: instantiating objects from textual description
/// The core concept is the one of **named builder**, which can build instances of classes given a **named parameter
/// map** (or named dictionary, using a different term).
/// A named parameter map is simply a collection of (key, value) pairs with a name.
/// See [below](#defining-a-named-parameter-map) for more details.

package io.github.ericmedvet.jnb.core;
