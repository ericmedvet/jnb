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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record Tree<L>(
    L content,
    List<Tree<L>> children
) implements Sized, Copyable<Tree<L>> {

  public final static String CHILDREN_START_DELIMITER = "(";
  public final static String CHILDREN_END_DELIMITER = ")";
  public final static String CHILDREN_SEPARATOR = ";";

  public Tree(L content) {
    this(content, List.of());
  }

  private static <L> Set<List<Integer>> lineages(List<Integer> currentLineage, Tree<L> tree) {
    return IntStream.range(0, tree.children.size())
        .mapToObj(i -> lineages(Utils.concat(currentLineage, i), tree.children.get(i)))
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Tree<L> copyOf() {
    return new Tree<>(
        content,
        children.stream().map(Tree::copyOf).toList()
    );
  }

  public Tree<L> descendant(List<Integer> lineage) {
    if (lineage.isEmpty()) {
      return this;
    }
    if (lineage.size() == 1) {
      return children.get(lineage.getFirst());
    }
    return children.get(lineage.getFirst()).descendant(lineage.subList(1, lineage.size()));
  }

  public int height() {
    return 1 + children.stream().mapToInt(Tree::height).max().orElse(0);
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  public Set<List<Integer>> lineages() {
    return lineages(List.of(), this);
  }

  @Override
  public int size() {
    return 1 + children().stream().mapToInt(Tree::size).sum();
  }

  @Override
  public String toString() {
    return content + (isLeaf() ? "" : (CHILDREN_START_DELIMITER + children.stream()
        .map(Tree::toString)
        .collect(Collectors.joining(CHILDREN_SEPARATOR)) + CHILDREN_END_DELIMITER));
  }
}