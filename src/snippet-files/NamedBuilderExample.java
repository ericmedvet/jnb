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

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.core.Param;

import java.util.List;
import java.util.function.Function;

public class NamedBuilderExample {
  public record Office( // @start region=builders
                        @Param("roomNumbers") List<Integer> roomNumbers,
                        @Param("head") Person head,
                        @Param("staff") List<Person> staff
  ) {}

  public record Person(
      @Param("name") String name,
      @Param(value = "age", dI = 45) int age
  ) {}

  public class Functions {
    private Functions() {
    }

    @Cacheable
    public static Function<String, String> shortener(
        @Param(value = "suffix", dS = ".") String suffix
    ) {
      return s -> s.charAt(0) + suffix;
    }
  } // @end region=builders

  public static void main(String[] args) { // @start region=using
    NamedBuilder<?> namedBuilder = NamedBuilder.empty()
        .and(NamedBuilder.fromClass(Office.class))
        .and(NamedBuilder.fromClass(Person.class))
        .and("f", NamedBuilder.fromUtilityClass(Functions.class));
    String description = """
        office(
          head = person(name = "Mario Rossi"; age = 43);
          staff = [
            person(name = Alice; age = 33);
            person(name = Bob; age = 25);
            person(name = Charlie)
          ];
          roomNumbers = [202:1:205]
        )
        """;
    Office office = (Office) namedBuilder.build(description);
    System.out.println(office);
    System.out.printf("The head's name is: %s%n", office.head().name());
    @SuppressWarnings("unchecked") Function<String, String> f = (Function<String, String>) namedBuilder.build(
        "f.shortener()"
    );
    System.out.printf("The head's short name is: %s%n", office.head().name()); // @end region=using
  }
}
