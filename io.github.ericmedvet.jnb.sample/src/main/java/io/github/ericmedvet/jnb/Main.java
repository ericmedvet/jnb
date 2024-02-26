/*-
 * ========================LICENSE_START=================================
 * jnb-sample
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
package io.github.ericmedvet.jnb;

import io.github.ericmedvet.jnb.core.*;
import java.util.List;

public class Main {

  private static final String S =
      """
office(
head = person(name = "Mario Rossi"; pet = pet(name = Fuffi; booleans = [true]));
staff = + [
person(name = Alice; age = 33; nicknames = [Puce; "The Cice"]; gender = f);
person(name = Bob);
person(name = Charlie; age = 38)
] + [person(name = Dane; age = 28)];
roomNumbers = [202:1:205]
)
""";

  public enum DayOfWeek {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN;

    @Override
    public String toString() {
      return "DayOfWeek{}";
    }
  }

  public enum Gender {
    M,
    F,
    OTHER
  }

  public record Office(
      @Param("roomNumbers") List<Integer> roomNumbers,
      @Param("head") Person head,
      @Param("staff") List<Person> staff,
      @Param(
              value = "spareStaff",
              dNPMs = {"person(name = Gigi)"})
          List<Person> spareStaff) {}

  public record Person(
      @Param(value = "", injection = Param.Injection.INDEX) int index,
      @Param("name") String name,
      @Param(value = "gender", dS = "m") Gender gender,
      @Param(value = "age", dI = 44) int age,
      @Param(value = "nice", dB = true) boolean nice,
      @Param("nicknames") List<String> nicknames,
      @Param(value = "pet", dNPM = "pet(name=Fido)") Pet pet,
      @Param(
              value = "preferredDays",
              dSs = {"mon", "fri"})
          List<DayOfWeek> preferredDays,
      @Param(value = "", injection = Param.Injection.MAP_WITH_DEFAULTS) ParamMap map) {}

  public record Pet(
      @Param("name") String name,
      @Param(value = "kind", dS = "dog") String kind,
      @Param(
              value = "legs",
              dIs = {4})
          List<Integer> legs,
      @Param(
              value = "booleans",
              dBs = {false, false})
          List<Boolean> booleans) {}

  public static void main(String[] args) {
    NamedBuilder<?> nb = NamedBuilder.empty()
        .and(NamedBuilder.fromClass(Office.class))
        .and(NamedBuilder.fromClass(Person.class))
        .and(NamedBuilder.fromClass(Pet.class));
    System.out.println(
        nb.build("person(name=eric;preferredDays=[mon;tue];age=44;pet=pet(name=\"simba\";legs=[2]))"));
    // System.exit(0);

    // Office office = (Office) nb.build(S);
    // System.out.println(office);
    // System.out.println(MapNamedParamMap.prettyToString(StringParser.parse("person(name = Eric;
    // preferredDays = [mon; fri])")));
    // System.out.println(MapNamedParamMap.prettyToString(StringParser.parse("person(name =
    // Andrew)")));
    // System.out.println(MapNamedParamMap.prettyToString(nb.fillWithDefaults(StringParser.parse("person(name =
    // Andrew)")), Integer.MAX_VALUE));
    // System.out.println(nb.fillWithDefaults(StringParser.parse("person(name = Andrew)")));
    // System.out.println(MapNamedParamMap.prettyToString(StringParser.parse(S)));
    // System.out.println(MapNamedParamMap.prettyToString(nb.fillWithDefaults(StringParser.parse(S))));

    InfoPrinter infoPrinter = new InfoPrinter();
    infoPrinter.print(nb, System.out);
  }
}
