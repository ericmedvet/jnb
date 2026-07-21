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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Function;
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
    SequencedMap<List<Integer>, L> lineageMap = lineages().stream()
        .collect(Utils.toSequencedMap(l -> descendant(l).label()));
    subtree.lineages()
        .forEach(
            l -> lineageMap.put(
                Utils.concat(lineage, l),
                subtree.descendant(l).label()
            )
        );
    return Tree.from(l -> Optional.ofNullable(lineageMap.get(l)));
  }
}