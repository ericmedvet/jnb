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

package io.github.ericmedvet.jnb;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.core.Param;

import java.util.List;

public class Starter {

  public record Office(
      @Param("roomNumbers") List<Integer> roomNumbers,
      @Param("head") Person head,
      @Param("staff") List<Person> staff
  ) {}

  public record Person(
      @Param("name") String name,
      @Param("age") int age
  ) {}

  public static class Persons {
    public static Person old(@Param("name") String name) {
      return new Person(name, 60);
    }

    public static Person young(@Param("name") String name) {
      return new Person(name, 18);
    }
  }

  public static void main(String[] args) {
    String description = """
        office(
          head = person(name = "Mario Rossi"; age = 43);
          staff = + [
            person(name = Alice; age = 33);
            person(name = Bob; age = 25);
            person(name = Charlie; age = 38)
          ] + [person(name = Dane; age = 28)];
          roomNumbers = [202:1:205] \s
        )
        """;
    NamedBuilder<?> namedBuilder = NamedBuilder.empty()
        .and(NamedBuilder.fromClass(Office.class))
        .and(NamedBuilder.fromClass(Person.class))
        .and(List.of("persons", "p"), NamedBuilder.fromUtilityClass(Persons.class));
    Office office = (Office) namedBuilder.build(description);
    System.out.println(office);
    System.out.printf("The head's name is: %s%n", office.head().name());
    System.out.printf("One young person is: %s%n", namedBuilder.build("p.young(name=Jack)"));
  }
}
