package io.github.ericmedvet.jnb;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.core.Param;

import java.util.List;

public class Main {

  public record Office(
      @Param("roomNumbers") List<Integer> roomNumbers,
      @Param("head") Person head,
      @Param("staff") List<Person> staff
  ) {}

  public record Person(
      @Param(value = "", injection = Param.Injection.INDEX) int index,
      @Param("name") String name,
      @Param(value = "age", dI = 44) int age,
      @Param("nicknames") List<String> nicknames
  ) {}

  private final static String S = """
      office(
              head = person(name = "Mario Rossi");
              staff = + [
                person(name = Alice; age = 33; nicknames = [Puce; "The Cice"]);
                person(name = Bob);
                person(name = Charlie; age = 38)
              ] + [person(name = Dane; age = 28)];
              roomNumbers = [202:1:205]
            )
      """;
  public static void main(String[] args) {
    NamedBuilder<?> namedBuilder = NamedBuilder.empty()
        .and(NamedBuilder.fromClass(Office.class))
        .and(NamedBuilder.fromClass(Person.class));
    Office office = (Office) namedBuilder.build(S);
    System.out.println(office);
  }
}
