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
import io.github.ericmedvet.jnb.core.ParamMap.Type;
import io.github.ericmedvet.jnb.core.parsing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

  private static final String S = // spotless:off
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

  public enum DayOfWeek {
    MON, TUE, WED, THU, FRI, SAT, SUN;

    @Override
    public String toString() {
      return "DayOfWeek{}";
    }
  }

  public enum Gender {
    M, F, OTHER
  }

  public record Office(
      @Param("roomNumbers") List<Integer> roomNumbers,
      @Param("head") Person head,
      @Param("staff") List<Person> staff,
      @Param(
          value = "spareStaff", dNPMs = {
              "person(name = Gigi)"}) List<Person> spareStaff
  ){
  }

  public record Person(
      @Param(value = "", injection = Param.Injection.INDEX) int index,
      @Param("name") String name,
      @Param(value = "gender", dS = "m") Gender gender,
      @Param(value = "age", dI = 44) int age,
      @Param(value = "nice", dB = true) boolean nice,
      @Param("nicknames") List<String> nicknames,
      @Param(
          value = "preferredDays", dSs = {
              "mon", "fri"}) List<DayOfWeek> preferredDays,
      @Param(value = "", injection = Param.Injection.MAP_WITH_DEFAULTS) ParamMap map
  ){
  }

  @Alias(
      name = "cat", value = "pet(kind = cat; owner = person(name = $ownerName; age = $age))", passThroughParams = {@PassThroughParam(name = "ownerName", type = Type.STRING, value = "ailo"), @PassThroughParam(name = "age", type = Type.INT, value = "45")
      })
  @Alias(
      name = "tiger", value = "pet(kind = tiger; owner = $tOwner)", passThroughParams = {@PassThroughParam(name = "tOwner", type = Type.NAMED_PARAM_MAP)})
  @Alias(name = "garfield", value = "cat(name = g; ownerName = gOwner)")
  public record Pet(
      @Param("name") String name,
      @Param(value = "kind", dS = "dog") String kind,
      @Param(
          value = "legs", dIs = {
              4}) List<Integer> legs,
      @Param("owner") Person owner,
      @Param(
          value = "booleans", dBs = {false, false}) List<Boolean> booleans
  ){
  }

  public static class Timed {

    private final Instant creationInstant;
    private final String name;

    @Cacheable
    public Timed(@Param("name") String name) {
      this.creationInstant = Instant.now();
      this.name = name;
    }
  }

  public static Function<String, String> shortener(
      @Param(value = "suffix", dS = ".") String suffix
  ) {
    return s -> s.charAt(0) + suffix;
  }

  public static void main(String[] args) throws ParseException, IOException {
    // justParse();

    NamedBuilder<?> nb = NamedBuilder.empty()
        .and(NamedBuilder.fromClass(Office.class))
        .and(NamedBuilder.fromClass(Person.class))
        .and(NamedBuilder.fromClass(Timed.class))
        .and(NamedBuilder.fromClass(Pet.class));
    @SuppressWarnings("unchecked") Function<String, String> f = (Function<String, String>) nb.build("f.doer()");
    // System.out.println(nb.build("person(name=eric;preferredDays=[mon;tue];age=44)"));
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
    // infoPrinter.print(nb, System.out);

    System.out.println(nb.build("cat(name = birba2)"));
    // System.exit(0);

    System.out.println(nb.build("cat(name = birba; ownerName = \"../gargamella\")"));
    System.out.println(nb.build("pet(name = simba; owner = person(name = eric))"));
    System.out.println(nb.build("garfield()"));
    System.out.println(nb.build("garfield(owner = person(name = maureen))"));
    System.out.println(nb.build("tiger(name = omo; tOwner = person(name = none))"));
    System.out.println(nb.build("timed(name = a)"));
    System.out.println(nb.build("timed(name = b)"));
    System.out.println(nb.build("timed(name = a)"));
    System.out.println(nb.build("person(name = eric; nicknames = [nn1; nn2])"));
    System.out.println(
        nb.build("$nn1 = nn1 $number = \"45\" person(name = $nn1; nicknames = [$nn1; nn2; $number])")
    );
  }

  private static String find(String s, String regex) {
    Matcher m = Pattern.compile(regex).matcher(s);
    if (m.find() && m.start() == 0) {
      return s.substring(0, m.end());
    }
    return null;
  }

  private static void justParse() throws ParseException, IOException {

    String s = "    toio %;\nhugo %;";
    Matcher matcher = Pattern.compile(
        "\\s*(%[^\\n\\r]*((\\r\\n)|(\\r)|(\\n))\\s*)*" + "[A-Za-z][A-Za-z0-9_]*" + "\\s*(%[^\\n\\r]*((\\r\\n)|" + "(\\r)|(\\n))\\s*)*"
    )
        .matcher(s);
    while (matcher.find()) {
      System.out.printf("`%s`%n", s.substring(matcher.start(), matcher.end()));
    }
    // System.exit(0);

    String s1 = // spotless:off
        """
            $age = 45
            $a = $age
            $owner = person(
              name = eric;
              % age = $a;
              friends = [
                toio %;
                % hugo;
                % ucio
              ]
            )
            """; // spotless:on
    String s2 = // spotless:off
        """
            animal(
              name = simba;
              nums = [1; 2; 3];
              age = 17
            )
            """; // spotless:on
    String s3 = // spotless:off
        """
            animal(
              name = "simba%33"; % comment with "quoted" content;
              nums = (name = [1;2;3]) * [n(); n()]; % comment
              owner = $owner;
              age = 17 % va;
            )
            """; // spotless:on
    String se = Files.readString(
        Path.of(
            "../jgea/io.github.ericmedvet.jgea" + ".experimenter/src/main/resources/exp-examples/mini-robot-vs-nav.txt"
        )
    );
    // System.out.println(StringParser.parse(s2));
    // System.out.println(StringParser.parse(s1 + s2));
    System.out.println(ParamMap.prettyToString(StringParser.parse(s1 + s3)));
    // System.out.println(MapNamedParamMap.prettyToString(StringParser.parse(se)));
    // NamedParamMap npm = StringParser.parse(se);
    // System.out.println(((List<?>) npm.value("runs", ParamMap.Type.NAMED_PARAM_MAPS)).size());
    System.exit(0);
  }
}
