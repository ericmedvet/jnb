/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
 * %%
 * Copyright (C) 2023 - 2026 Eric Medvet
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
package io.github.ericmedvet.jnb.datastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// An ordered tree with nodes labeled with non-null labels of type `L`.
///
/// @param label the label of the root node of this tree
/// @param children the ordered list of trees being children of the root of this tree
/// @param <L> the type of labels of nodes
public record Tree<L>(
    L label,
    List<Tree<L>> children
) implements Sized, Copyable<Tree<L>> {

  /// The string used for delimiting the children in the string representation of a tree given by
  /// the [#toString()] method.
  public final static String CHILDREN_START_DELIMITER = "(";
  /// The string used for delimiting the children in the string representation of a tree given by
  /// the [#toString()] method.
  public final static String CHILDREN_END_DELIMITER = ")";
  /// The string used for separating children in the string representation of a tree given by the
  /// [#toString()] method.
  public final static String CHILDREN_SEPARATOR = ";";

  /// Builds a tree with one single leaf node.
  ///
  /// @param label the label of the root node
  public Tree(L label) {
    this(label, List.of());
  }

  /// Builds a tree from a function that maps lineages to labels. For building the new tree, this
  /// method invokes the function `lineageMapper` repeatedly starting from the root (i.e., with
  /// lineage `[]`). For every higher branch, it invokes the `lineageMapper` starting from the index
  /// 0 and stops, for that level, when the mapper returns an empty optional. The method invokes the
  /// function in a depth first way.
  ///
  /// @param lineageMapper a function that takes a *lineage* (see [#descendant(List)]) and returns
  /// the label that is to be used in the new tree at that lineage
  /// @param <L>           the type of labels of nodes
  /// @return the built tree
  /// @throws IllegalArgumentException if the `lineageMapper` gives an empty `Optional` for the
  /// empty lineage `[]`, which should instead give the label of the root
  public static <L> Tree<L> from(
      Function<? super List<Integer>, Optional<? extends L>> lineageMapper
  ) {
    L label = lineageMapper.apply(List.of())
        .orElseThrow(() -> new IllegalArgumentException("Lineage mapper does not map the root"));
    int i = 0;
    List<Tree<L>> children = new ArrayList<>();
    while (lineageMapper.apply(List.of(i)).isPresent()) {
      final int j = i;
      children.add(
          from(lineageMapper.compose(lineage -> Utils.<Integer>concat(List.of(j), lineage)))
      );
      i = i + 1;
    }
    return new Tree<>(label, Collections.unmodifiableList(children));
  }

  private static <L> Stream<List<Integer>> lineages(List<Integer> currentLineage, Tree<L> tree) {
    return IntStream.range(0, tree.children.size())
        .mapToObj(
            i -> Stream.concat(
                Stream.of(Utils.concat(currentLineage, i)),
                lineages(Utils.concat(currentLineage, i), tree.children.get(i))
            ).collect(Collectors.toSet())
        )
        .flatMap(Set::stream);
  }

  /// Returns the `index`-th child of this tree.
  ///
  /// @param index the index of the child to return
  /// @return the `index`-th child of this tree
  public Tree<L> child(int index) {
    return children.get(index);
  }

  /// Creates a shallow copy of this tree. The children are copies of this tree children; the label
  /// is the same label of this tree root label.
  ///
  /// @return a copy of this tree
  @Override
  public Tree<L> copyOf() {
    return new Tree<>(
        label,
        children.stream().map(Tree::copyOf).toList()
    );
  }

  /// Returns the list of all the labels in this tree obtained through a depth first visit.
  ///
  /// @return the list of all labels in this tree
  public List<L> depthFirstLabels() {
    return Stream.concat(
        Stream.of(label),
        children.stream().flatMap(t -> t.depthFirstLabels().stream())
    ).toList();
  }

  /// Returns the tree being located at the given lineage coordinate. Internally, it calls
  /// [#descendant(List)].
  ///
  /// @param lineage the sequence of zero-based indexes to look at in this tree
  /// @return the tree at the given lineage, if any
  /// @throws IndexOutOfBoundsException if there is no child at one of the indexes of the provided
  /// lineage
  public Tree<L> descendant(int... lineage) {
    return descendant(Arrays.stream(lineage).boxed().toList());
  }

  /// Returns the tree being located at the given lineage coordinate. A lineage coordinate, simply
  /// *lineage*, is a sequence of zero-based indexes identifying the child to choose starting from
  /// the root of this tree. The empty lineage `[]` refers to this tree. The `[0]` lineage
  /// represents the first child of this tree. The `[1,0]` lineage represents the first child of the
  /// second child of this tree.
  ///
  /// @param lineage the sequence of zero-based indexes to look at in this tree
  /// @return the tree at the given lineage, if any
  /// @throws IndexOutOfBoundsException if there is no child at one of the indexes of the provided
  /// lineage
  public Tree<L> descendant(List<Integer> lineage) {
    if (lineage.isEmpty()) {
      return this;
    }
    if (lineage.size() == 1) {
      return children.get(lineage.getFirst());
    }
    return children.get(lineage.getFirst()).descendant(lineage.subList(1, lineage.size()));
  }

  /// Returns the height of this tree, one for trees with one single node.
  ///
  /// @return the height of this tree, always >= 1
  public int height() {
    return 1 + children.stream().mapToInt(Tree::height).max().orElse(0);
  }

  /// Tests if this tree has a single node, i.e., a leaf node.
  ///
  /// @return true if this tree has a single node
  public boolean isLeaf() {
    return children.isEmpty();
  }

  /// Returns the list of the labels of the leaves of this tree.
  ///
  /// @return the list of the labels of the leaves of this tree
  public List<L> leafLabels() {
    if (isLeaf()) {
      return List.of(label);
    }
    return children.stream().flatMap(t -> t.leafLabels().stream()).toList();
  }

  /// Returns all the lineages (see [#descendant(List)]) of this tree, in a depth first predictable
  /// order. A copy of this tree can be rebuilt from the returned lineages through the
  /// [Tree#from(Function)] method, namely with `from(l -> Optional.of(descendant(l).label()))`.
  ///
  /// @return all the lineages of this tree
  public SequencedSet<List<Integer>> lineages() {
    return Stream.concat(Stream.of(List.<Integer>of()), lineages(List.of(), this))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /// Returns a new tree with the same structure of this tree where every label is obtained by
  /// mapping the corresponding label of this tree through the provided `mapper`.
  ///
  /// @param mapper a function for mapping this tree labels to the built tree labels
  /// @param <K>    the type of labels of nodes of the returned tree
  /// @return a new tree with same structure of this tree and labels mapped through the provided
  /// mapper
  public <K> Tree<K> map(Function<? super L, ? extends K> mapper) {
    //noinspection unchecked
    return new Tree<>(
        mapper.apply(label),
        children.stream().map(c -> (Tree<K>) c.map(mapper)).toList()
    );
  }

  /// Returns the number of nodes in this tree.
  ///
  /// @return the number of nodes in this tree
  @Override
  public int size() {
    return 1 + children().stream().mapToInt(Tree::size).sum();
  }

  /// Returns a string representation of this tree. It is built recursively by concatenating this
  /// tree label followed by, if this is not a leaf node, {@value #CHILDREN_START_DELIMITER}, the
  /// list of children string representations separated by {@value #CHILDREN_SEPARATOR}, and
  /// {@value #CHILDREN_END_DELIMITER}.
  ///
  /// @return a string representation of this tree
  @Override
  public String toString() {
    return label + (isLeaf() ? "" : (CHILDREN_START_DELIMITER + children.stream()
        .map(Tree::toString)
        .collect(Collectors.joining(CHILDREN_SEPARATOR)) + CHILDREN_END_DELIMITER));
  }

  /// Returns a new tree built from this tree with the provided `subtree` put at the provided
  /// `lineage` of this tree.
  ///
  /// @param subtree the tree to be appended at the specified lineage
  /// @param lineage the lineage where to append the subtree
  /// @return the new tree built by putting the provided subtree at the provided lineage in this
  /// tree
  public Tree<L> withAt(Tree<? extends L> subtree, List<Integer> lineage) {
    return Tree.from(l -> {
      if (startsWith(l, lineage)) {
        return ifSuccess(() -> subtree.descendant(l.subList(lineage.size(), l.size())).label());
      }
      return ifSuccess(() -> descendant(l).label());
    });
  }

  private static <K> Optional<K> ifSuccess(Callable<K> supplier) {
    try {
      return Optional.of(supplier.call());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static <K> boolean startsWith(List<K> list, List<K> prefix) {
    if (list.size() < prefix.size()) {
      return false;
    }
    return list.subList(0, prefix.size()).equals(prefix);
  }

  /// A parser which produces tree parsing strings. It has to be configured through four parameters
  /// (see the constructor):
  /// - a list of `NodeParser` for parsing labels of non-terminal nodes
  /// - a list of `NodeParser` for parsing labels of terminal (leaf) nodes
  /// - a `BiPredicate` for testing whether a given arity is acceptable for a given non-terminal
  /// label
  /// - a `Configuration` specifying parsing details, e.g., children separator For example, it can
  /// parse the string `"a(b;c(d))"` to the tree:
  /// ```
  /// a
  /// └b
  /// └c
  ///  └d
  /// ```
  ///
  /// @param <L>  the type of labels of nodes
  /// @param <NT> the type of labels of non-terminal nodes
  /// @param <T>  the type of labels of terminal nodes
  public static class StringParser<L, NT extends L, T extends L> {

    private static final String VOID_REGEX = "\\s*";
    private final List<? extends NodeParser<? extends NT>> nonTerminalParsers;
    private final List<? extends NodeParser<? extends T>> terminalParsers;
    private final BiPredicate<NT, Integer> arityPredicate;
    private final Configuration configuration;

    /// Builds a new parser which will use the provided arguments for parsing trees from strings.
    /// The order of `NodeParser`s in the lists `nonTerminalParsers` and `terminalParsers` matter.
    /// The parser works recursively as follows (in [#parse(String)]):
    /// 1. it starts from the first character at index `i = 0`;
    /// 2. attempts to parse a label at `i` using first the non-terminal parsers (in order) and then
    /// the terminal parsers:
    ///   - if a non-terminal parser matches a label, it looks for a
    /// [Configuration#childrenStartDelimiter()], then parses inner nodes, separated by a
    /// [Configuration#childrenStartDelimiter()] and terminated by a
    /// [Configuration#childrenEndDelimiter()]; if it does not find a start delimiter, goes to next
    /// case;
    ///   - otherwise, if a terminal parser matches a label, returns a tree with a leaf node labeled
    /// with the parsed label;
    ///   - otherwise, throws a `NoSuchElementException`. When parsing label nodes, the parser
    /// permits empty spaces if [Configuration#allowVoid()] is true.
    ///
    /// @param nonTerminalParsers a list of parsers for labels of non-terminal nodes
    /// @param terminalParsers    a list of parsers for labels of terminal nodes
    /// @param arityPredicate     a predicate testing whether a given arity (number of children) is
    /// legit for a non-terminal node labeled with a given label
    /// @param configuration      the configuration for this parser
    public StringParser(
        List<? extends NodeParser<? extends NT>> nonTerminalParsers,
        List<? extends NodeParser<? extends T>> terminalParsers,
        BiPredicate<NT, Integer> arityPredicate,
        Configuration configuration
    ) {
      this.nonTerminalParsers = nonTerminalParsers;
      this.terminalParsers = terminalParsers;
      this.arityPredicate = arityPredicate;
      this.configuration = configuration;
    }

    private static Optional<Token<String>> findAt(
        String string,
        int i,
        String regex,
        boolean allowVoid
    ) {
      Matcher matcher = Pattern
          .compile(allowVoid ? (VOID_REGEX + regex + VOID_REGEX) : regex)
          .matcher(string);
      if (!matcher.find(i) || matcher.start() != i) {
        return Optional.empty();
      }
      return Optional.of(
          new Token<>(
              allowVoid ? string.substring(matcher.start(), matcher.end())
                  .replaceAll("\\A" + VOID_REGEX, "")
                  .replaceAll(VOID_REGEX + "\\z", "") : string.substring(matcher.start(), matcher.end()),
              i,
              matcher.end()
          )
      );
    }

    /// Parses the provided string for producing a `Tree`.
    ///
    /// @param string the string to parse
    /// @return the parsed tree
    /// @throws java.util.NoSuchElementException if a tree cannot be parsed from the string
    public Tree<L> parse(String string) {
      return parseNode(string, 0).orElseThrow().content;
    }

    private static class ParseException extends RuntimeException {

      public enum Component { LABEL, START_DELIMITER, SEPARATOR, END_DELIMITER }

      private final Component component;

      public ParseException(Component component) {
        this.component = component;
      }

      public Component component() {
        return component;
      }
    }

    private Optional<Token<Tree<L>>> parseNode(String string, int i) {
      int finalI = i;
      Optional<? extends Token<? extends NT>> ntToken = nonTerminalParsers.stream()
          .map(np -> np.parse(string, finalI))
          .filter(Optional::isPresent)
          .findFirst()
          .map(Optional::orElseThrow);
      List<Tree<L>> children = new ArrayList<>();
      try {
        if (ntToken.isPresent()) {
          i = ntToken.get().end();
          i = findAt(
              string,
              i,
              configuration.childrenStartDelimiter,
              configuration.allowVoid
          ).orElseThrow(() -> new ParseException(ParseException.Component.START_DELIMITER)).end;
          while (true) {
            if (!children.isEmpty()) {
              Optional<Token<String>> separator = findAt(
                  string,
                  i,
                  configuration.childrenSeparator,
                  configuration.allowVoid
              );
              if (separator.isEmpty()) {
                break;
              }
              i = separator.get().end;
            }
            Optional<Token<Tree<L>>> child = parseNode(string, i);
            i = child.orElseThrow().end;
            children.add(child.orElseThrow().content);
          }
          i = findAt(
              string,
              i,
              configuration.childrenEndDelimiter,
              configuration.allowVoid
          ).orElseThrow(() -> new ParseException(ParseException.Component.END_DELIMITER)).end;
          if (!arityPredicate.test(ntToken.get().content(), children.size())) {
            throw new IllegalArgumentException(
                "Wrong arity %d for operator %s".formatted(
                    children.size(),
                    ntToken.get().content()
                )
            );
          }
          return Optional.of(
              new Token<>(
                  new Tree<>(ntToken.get().content(), children),
                  finalI,
                  i
              )
          );
        }
      } catch (ParseException e) {
        if (!e.component().equals(ParseException.Component.START_DELIMITER)) {
          throw new NoSuchElementException();
        }
      }
      Optional<? extends Token<? extends T>> tToken = terminalParsers.stream()
          .map(np -> np.parse(string, finalI))
          .filter(Optional::isPresent)
          .findFirst()
          .map(Optional::orElseThrow);
      if (tToken.isPresent()) {
        return Optional.of(
            new Token<>(
                new Tree<>(tToken.get().content()),
                i,
                tToken.get().end()
            )
        );
      }
      return Optional.empty();
    }

    /// A parser of node labels from strings.
    ///
    /// @param <L> the type of labels produced upon parsing
    public interface NodeParser<L> {

      /// Builds a list of `NodeParser`s, one for each value of the provided `enumClass`, which will
      /// produce enum values of that class when matching their corresponding string
      /// representations. Optionally, each parser can allow for empty content (i.e.,
      /// {@value VOID_REGEX}) before the match, by setting `allowVoid`.
      ///
      /// @param enumClass the class of the enum to build the parsers for
      /// @param allowVoid a flag for setting whether to accept empty content before a match
      /// @param <E>       the type of the enum values produced by the parsers
      /// @return a list of `NodeParser`s, one for each value of the provided `enumClass`
      static <E extends Enum<E>> List<? extends NodeParser<? extends E>> fromEnum(
          Class<E> enumClass,
          boolean allowVoid
      ) {
        SequencedMap<String, E> namedValues = Arrays.stream(enumClass.getEnumConstants())
            .collect(
                Collectors.toMap(
                    Enum::toString,
                    e -> e,
                    (e1, _) -> e1,
                    LinkedHashMap::new
                )
            );
        return namedValues.keySet()
            .stream()
            .map(
                n -> (NodeParser<? extends E>) fromRegex(
                    Pattern.quote(n),
                    namedValues::get,
                    allowVoid
                )
            )
            .toList();
      }

      /// Builds a `NodeParser` which works by looking for a match of a provided `regex` and the
      /// transforming that match in a label through a provided `builder`. Optionally, it can allow
      /// for empty content (i.e., {@value VOID_REGEX}) before the match, by setting `allowVoid`.
      ///
      /// @param regex     the regex for matching the content to extract
      /// @param builder   the function mapping a matched content to a label
      /// @param allowVoid a flag for setting whether to accept empty content before a match
      /// @param <L>       the type of parsed labels
      /// @return a parser based on the provided `regex` and `builder`
      static <L> NodeParser<? extends L> fromRegex(
          String regex,
          Function<String, ? extends L> builder,
          boolean allowVoid
      ) {
        return (s, i) -> findAt(s, i, regex, allowVoid)
            .map(
                t -> new Token<>(
                    builder.apply(t.content),
                    t.start,
                    t.end
                )
            );
      }

      /// Attempts to parse a token in the provided string `s` at index `i`
      ///
      /// @param s the string being parsed
      /// @param i the index of the string to parse at
      /// @return the result of the parsing, empty if no token was found, with a `Token` otherwise
      Optional<Token<L>> parse(String s, int i);
    }

    /// A configuration for a `StringParser`, specifying what regexes to use as initial delimiter
    /// for the list of children, final delimiter, and inner separator.
    ///
    /// @param childrenStartDelimiter the regex for initial delimiter for the list of children
    /// @param childrenEndDelimiter   the regex for final delimiter for the list of children
    /// @param childrenSeparator      the regex for separator for the list of children
    /// @param allowVoid              a flag for setting whether to accept empty content before a
    /// match
    public record Configuration(
        String childrenStartDelimiter,
        String childrenEndDelimiter,
        String childrenSeparator,
        boolean allowVoid
    ) {

      /// The default configuration, where the initial delimiter is
      /// {@value CHILDREN_START_DELIMITER}, the final delimiter is {@value CHILDREN_END_DELIMITER},
      /// and the separator is {@value CHILDREN_SEPARATOR}. The `allowVoid` flag is set to true.
      public static final Configuration DEFAULT = new Configuration(
          Pattern.quote(Tree.CHILDREN_START_DELIMITER),
          Pattern.quote(Tree.CHILDREN_END_DELIMITER),
          Pattern.quote(Tree.CHILDREN_SEPARATOR),
          true
      );
    }

    /// A token produced while parsing a string and holding some value (i.e., *content*) of type
    /// `L`.
    ///
    /// @param content the content of the token
    /// @param start   the index of the first char (included) of the token in the string
    /// @param end     the index of the last char (excluded) of the token in the string
    /// @param <L>     the type of the content of the token
    public record Token<L>(L content, int start, int end) {

    }

  }
}