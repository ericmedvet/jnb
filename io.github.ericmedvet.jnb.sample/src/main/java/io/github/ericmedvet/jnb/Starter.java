/*-
 * ========================LICENSE_START=================================
 * jnb-sample
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
package io.github.ericmedvet.jnb;

import io.github.ericmedvet.jnb.core.*;
import java.util.List;

public class Starter {

  @Discoverable
  public record Office(
      @Param("roomNumbers") List<Integer> roomNumbers,
      @Param("head") Person head,
      @Param("staff") List<Person> staff
  ) {}

  @Discoverable
  @Alias(name = "namedPerson", value = "person(name = name)")
  public record Person(
      @Param("name") String name, @Param("age") int age, @Param("nicknames") List<String> nicknames
  ) {}

  @Discoverable(prefixTemplate = "p|person")
  public static class Persons {

    private Persons() {
    }

    @Alias(name = "mathusalem", value = "old(name = Mathusalem; age = 199)")
    @Alias(name = "friendlyMathusalem", value = "mathusalem(nicknames=[Math])")
    public static Person old(
        @Param("name") String name,
        @Param(value = "age", dI = 55) int age,
        @Param("nicknames") List<String> nicknames
    ) {
      return new Person(name, age, nicknames);
    }

    public static Person young(@Param("name") String name, @Param(value = "age", dI = 18) int age) {
      return new Person(name, age, List.of());
    }
  }

  public static void main(String[] args) {
    String description = // spotless:off
        """
            office(       % this is a comment
              head = person(name = "Mario Rossi"; age = 43);
              staff = + [
                person(name = Alice; age = 33; nicknames = [Puce; "The Cice"]);
                person(name = Bob; age = 25);
                person(name = Charlie; age = 38)
              ] + [person(name = Dane; age = 28)];
              % another comment
              roomNumbers = [201:2:210] \s
            )
            """; // spotless:on
    NamedBuilder<?> namedBuilder = NamedBuilder.fromDiscovery("io.github.ericmedvet");
    System.out.println(namedBuilder);
    Office office = (Office) namedBuilder.build(description);
    System.out.println(office);
    System.out.printf("The head's name is: %s%n", office.head().name());
    System.out.printf("One young person is: %s%n", namedBuilder.build("p.young(name=Jack)"));
    System.out.printf("Mathusalem is: %s%n", namedBuilder.build("p.mathusalem()"));
    System.out.printf("Friendly mathusalem is: %s%n", namedBuilder.build("p.friendlyMathusalem(age = 45)"));
  }
}
