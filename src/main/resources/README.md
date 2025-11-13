# jnb - Java Named Builder

**jnb** is a Java library for building instances of classes given textual descriptions formatted in a proper way.

More specifically, jnb provides a few interfaces and classes for doing the following key things:
1. **annotating** an existing class or method to be used as a builder: the key artifacts for this are the annotations [`@Param`](io.github.ericmedvet.jnb.core/src/main/java/io/github/ericmedvet/jnb/core/Param.java) and [`@BuilderMethod`](io.github.ericmedvet.jnb.core/src/main/java/io/github/ericmedvet/jnb/core/BuilderMethod.java);
2. **parsing** a textual description into an object storing the information needed to invoke a builder: the key artifact here is the interface [`NamedParamMap`](io.github.ericmedvet.jnb.core/src/main/java/io/github/ericmedvet/jnb/core/NamedParamMap.java);
3. **building** a builder automatically from annotated class: the key artifact here is the [`NamedBuilder`](io.github.ericmedvet.jnb.core/src/main/java/io/github/ericmedvet/jnb/core/NamedBuilder.java).

The three steps, and the corresponding key artifacts, are explained below.

## Example

Very in brief, the intended usage is the one represented in [this example](io.github.ericmedvet.jnb.sample/src/main/java/io/github/ericmedvet/jnb/Starter.java), which is mostly self-explanatory:
```java
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
  public static Person young(@Param("name") String name) {
    return new Person(name, 18);
  }

  public static Person old(@Param("name") String name) {
    return new Person(name, 60);
  }
}

public static void main(String[] args) {
  String description = """
      office(
        head = person(name = "Mario Rossi"; age = 43);
        staff = [
          person(name = Alice; age = 33);
          person(name = Bob; age = 25);
          person(name = Charlie; age = 38)
        ];
        roomNumbers = [202:1:205]
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
```

Note that in this example there are 3 ways for building a person: the corresponding names are `person`, `persons.young`, and `persons.old`.

## Usage

### Preparation

Add this to your `pom.xml`:
```xml
<dependency>
    <groupId>io.github.ericmedvet</groupId>
    <artifactId>jnb.core</artifactId>
    <version>${project.version}</version>
</dependency>
```

If your Java project uses modules, you will **need** to modify your `module-info.java` by **requiring** the jnb core module and by **opening** every package you need to annotate the jnb core module (this is required because jnb uses reflection).
Example:
```
module io.github.ericmedvet.jnb.sample {
  requires io.github.ericmedvet.jnb.core;
  opens your.project.package to io.github.ericmedvet.jnb.core;
}
```

### Overview

The core concept is the one of **named builder**, which can build instances of classes given a **named parameter map** (or named dictionary, using a different term).
A named parameter map is simply a collection of (key, value) pairs with a name.
See [below](#defining-a-named-parameter-map) for more details.

### Annotating a class or method

You can annotate a method or a constructor (also of a `record`) in order to make it discoverable by the methods `fromClass()` and `fromUtilityClass()` of [`NamedBuilder`](io.github.ericmedvet.jnb.core/src/main/java/io/github/ericmedvet/jnb/core/NamedBuilder.java).

For example:
```java
public static Person young(@Param("name") String name, @Param(value = "age", dI = 43) int age) {
  return new Person(name, 18);
}
```
will result in a named builder where the name is `young` (possibly with a prefix, as in the previous example) and the expected parameters are `name` and, optionally (in the sense that there is a default value of `43`), `age`.

### Defining a named parameter map

A **named parameter map** is a map (or dictionary, in other terms) with a name.
It can be described with a string adhering the following human- and machine-readable format described by the following grammar:
```
<npm> ::= <n>(<nps>)
<nps> ::= ∅ | <np> | <nps>;<np>
<np> ::= <n>=<npm> | <n>=<d> | <n>=<s> | <n>=<lnpm> | <n>=<ld> | <n>=<ls>
<lnpm> ::= (<np>)*<lnpm> | <i>*<lnpm> | +<lnpm>+<lnpm> | [<npms>]
<ld> ::= [<d>:<d>:<d>] | [<ds>]
<ls> ::= [<ss>]
<npms> ::= ∅ | <npm> | <npms>;<npm>
<ds> ::= ∅ | <d> | <ds>;<d>
<ss> ::= ∅ | <s> | <ss>;<s> | <s>+<s>
```
where:
- `<npm>` is a named parameter map;
- `<n>` is a name, i.e., a string in the format `[A-Za-z][.A-Za-z0-9_]*`;
- `<s>` is a string in the format `([A-Za-z][A-Za-z0-9_]*)|("[^"]+")`;
- `<d>` is a number in the format `(-?[0-9]+(\.[0-9]+)?)|(-?Infinity)`;
- `<i>` is a number in the format `[0-9]+`;
- `∅` is the empty string.

The format is reasonably robust to spaces and line-breaks.
Moreover, you can include line comments in the string describing the map, with the syntax `% comment`, to be put reasonably everywhere in the line.

An example of a syntactically valid named parameter map is:
```
car(dealer = Ferrari; price = 45000)
```
where `dealer` and `price` are parameter names and `Ferrari` and `45000` are parameter values.
`car` is the name of the map.

Another, more complex example is:
```
office(
  head = person(name = "Mario Rossi"; age = 43);
  staff = [
    person(name = Alice; age = 33);
    person(name = Bob; age = 25);
    person(name = Charlie; age = 38)
  ];
  roomNumbers = [1:2:10]  % this will be read exactly as [1; 3; 5; 7; 9]
)
```
In this case, the `head` parameter of `office` is valued with another named parameter map: `person(name = "Mario Rossi"; age = 43)`.

##### The `*` and `+` operators

Note the possible use of `*` for specifying arrays of named parameter maps (broadly speaking, collections of them) in a more compact way.
For example, `2 * [dog(name = simba); dog(name = gass)]` corresponds to `[dog(name = simba); dog(name = gass); dog(name = simba); dog(name = gass)]`.
A more complex case is the one of left-product that takes a parameter $p$ valued with an array $v_1, \dots, v_k$ (on the left) and an array $m_1, \dots, m_n$ of named parameter maps (on the right) and results in the array of named parameter maps $m^\prime_{1,1}, \dots, m^\prime_{1,k}, \dots, m^\prime_{n,1}, \dots, m^\prime_{n,k}$ where each $m'_{i,j}$ is the map $m_i$ with a parameter $p$ valued $v_k$.
```
(size = [m; s; xxs]) * [hoodie(color = red)]
```
corresponds to:
```
[
  hoodie(color = red; size = m);
  hoodie(color = red; size = s);
  hoodie(color = red; size = xxs)
]
```

The `+` operator simply concatenates arrays.
Note that the first array has to be prefixed with `+` too.

An example of combined use of `*` and `+` is:
```
+ (size = [m; s; xxs]) * [hoodie(color = red)] + [hoodie(color = blue; size = m)]
```
that corresponds to:
```
[
  hoodie(color = red; size = m);
  hoodie(color = red; size = s);
  hoodie(color = red; size = xxs);
  hoodie(color = blue; size = m)
]
```


### Building and using a `NamedBuilder`

In the typical case, you will build a `NamedBuilder` by chaining together a few other named builders, each built automatically with the methods `fromClass()` and `fromUtilityClass()` of [`NamedBuilder`](io.github.ericmedvet.jnb.core/src/main/java/io/github/ericmedvet/jnb/core/NamedBuilder.java), as shown in the [example above](#example).

### Usages

This project is used in three other projects:
- [JGEA](https://github.com/ericmedvet/jgea)
- [2D-MR-Sim](https://github.com/ericmedvet/2dmrsim)
- [2d-robot-evolution](https://github.com/ericmedvet/2d-robot-evolution)