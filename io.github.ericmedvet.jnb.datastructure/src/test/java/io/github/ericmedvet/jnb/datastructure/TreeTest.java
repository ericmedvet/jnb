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

import static org.assertj.core.api.Assertions.*;

import io.github.ericmedvet.jnb.datastructure.Tree.StringParser;
import io.github.ericmedvet.jnb.datastructure.Tree.StringParser.Configuration;
import io.github.ericmedvet.jnb.datastructure.Tree.StringParser.NodeParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreeTest {

  private final static StringParser<Character, Character, Character> P = new StringParser<>(
      List.of(NodeParser.fromRegex("[a-zA-Z]", s -> s.charAt(0), true)),
      List.of(NodeParser.fromRegex("[a-zA-Z]", s -> s.charAt(0), true)),
      (_, _) -> true,
      Configuration.DEFAULT
  );

  private static Tree<Character> t(String s) {
    return P.parse(s);
  }

  @Test
  void copyOf() {
    Tree<Character> t = t("a(b;c)");
    assertThat(t.copyOf())
        .as("copy is not ==")
        .isNotSameAs(t);
    assertThat(t.copyOf())
        .as("copy is equals()")
        .isEqualTo(t);
  }

  @Test
  void descendant() {
    Tree<Character> t = t("a(b;c(d))");
    assertThat(t.descendant(List.of()))
        .as("empty lineage is the tree")
        .isEqualTo(t);
    assertThat(t.descendant(List.of(1)))
        .as("[1] descendant of a(b;c(d)) is c(d)")
        .isEqualTo(t("c(d)"));
    assertThat(t.descendant(List.of(1, 0)))
        .as("[1,0] descendant of a(b;c(d)) is d")
        .isEqualTo(t("d"));
  }

  @Test
  void depthFirstLabels() {
    assertThat(t("a(b;c(d))").depthFirstLabels())
        .as("visit nodes of a(b;c(d))")
        .containsExactly('a', 'b', 'c', 'd');
  }

  @Test
  void parse() {
    assertThat(
        new StringParser<>(
            List.of(NodeParser.fromRegex("[a-z]", s -> s.charAt(0), true)),
            List.of(NodeParser.fromRegex("[A-Z]", s -> s.charAt(0), true)),
            (_, _) -> true,
            Configuration.DEFAULT
        ).parse("a(B;c(D))")
    )
        .as("parsing NT-T disjoint with default configuration")
        .isEqualTo(
            new Tree<>(
                'a',
                List.of(
                    new Tree<>('B'),
                    new Tree<>(
                        'c',
                        List.of(
                            new Tree<>('D')
                        )
                    )
                )
            )
        );
    assertThat(
        new StringParser<>(
            List.of(NodeParser.fromRegex("[a-z]", s -> s.charAt(0), true)),
            List.of(NodeParser.fromRegex("[a-z]", s -> s.charAt(0), true)),
            (_, _) -> true,
            Configuration.DEFAULT
        ).parse("a(b;c(d))")
    )
        .as("parsing NT=T with default configuration")
        .isEqualTo(
            new Tree<>(
                'a',
                List.of(
                    new Tree<>('b'),
                    new Tree<>(
                        'c',
                        List.of(
                            new Tree<>('d')
                        )
                    )
                )
            )
        );
  }

  @Test
  void map() {
    assertThat(t("a(b;c(d))").map(c -> (char) (c + 1)))
        .as("map to incremented char")
        .isEqualTo(t("b(c;d(e))"));
    assertThat(t("a(b;c(d))").map(Character::toUpperCase))
        .as("map to uppercased char")
        .isEqualTo(t("A(B;C(D))"));
  }

  @Test
  void withAt() {
    assertThat(t("a(b;c)").withAt(t("d(e;f)"), List.of(1)))
        .as("replace leaf with subtree")
        .isEqualTo(t("a(b;d(e;f))"));
    assertThat(t("a(b;c(d;e(f;g)))").withAt(t("a(a)"), List.of(1)))
        .as("replace subtree with chain")
        .isEqualTo(t("a(b;a(a))"));
  }

  @Test
  void from() {
    Map<List<Integer>, Character> lineageMap = Map.ofEntries(
        Map.entry(List.of(), 'a'),
        Map.entry(List.of(0), 'b'),
        Map.entry(List.of(1), 'c'),
        Map.entry(List.of(1, 0), 'd')
    );
    assertThat(Tree.from(l -> Optional.ofNullable(lineageMap.get(l))))
        .as("from lineage of a(b,c(d))")
        .isEqualTo(t("a(b;c(d))"));
  }

  @Test
  void leafLabels() {
    assertThat(t("a(b;c(d))").leafLabels())
        .as("visit labels of a(b;c(d))")
        .containsExactly('b', 'd');
  }

  @Test
  void height() {
    assertThat(t("a").height())
        .as("height of leaf")
        .isEqualTo(1);
    assertThat(t("a(b;c)").height())
        .as("height of a(b;c)")
        .isEqualTo(2);
    assertThat(t("a(b;c(d))").height())
        .as("height of a(b;c(d))")
        .isEqualTo(3);
  }

  @Test
  void isLeaf() {
    assertThat(t("a").isLeaf())
        .as("a is leaf")
        .isTrue();
    assertThat(t("a(b;c)").isLeaf())
        .as("a(b;c) is not leaf")
        .isFalse();
    assertThat(t("a(b;c(d))").isLeaf())
        .as("a(b;c(d)) is not leaf")
        .isFalse();
  }

  @Test
  void lineages() {
    assertThat(t("a(b;c(d))").lineages())
        .as("lineage of a(b;c(d))")
        .containsExactlyInAnyOrder(
            List.of(),
            List.of(0),
            List.of(1),
            List.of(1, 0)
        );
  }

  @Test
  void size() {
    assertThat(t("a").size())
        .as("size of leaf")
        .isEqualTo(1);
    assertThat(t("a(b;c)").size())
        .as("size of a(b;c)")
        .isEqualTo(3);
    assertThat(t("a(b;c(d))").size())
        .as("size of a(b;c(d))")
        .isEqualTo(4);
  }
}