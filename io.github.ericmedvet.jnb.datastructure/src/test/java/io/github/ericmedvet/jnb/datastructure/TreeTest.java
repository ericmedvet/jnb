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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreeTest {

  @Test
  void copyOf() {
    Tree<Character> t = new Tree<>(
        'a',
        List.of(
            new Tree<>('b'),
            new Tree<>('c')
        )
    );
    assertThat(t.copyOf())
        .as("copy is not ==")
        .isNotSameAs(t);
    assertThat(t.copyOf())
        .as("copy is equals()")
        .isEqualTo(t);
  }

  @Test
  void descendant() {
    Tree<Character> t = new Tree<>(
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
    );
    assertThat(t.descendant(List.of()))
        .as("empty lineage is the tree")
        .isEqualTo(t);
    assertThat(t.descendant(List.of(1)))
        .as("[1] descendant of a(b;c(d)) is c(d)")
        .isEqualTo(
            new Tree<>(
                'c',
                List.of(
                    new Tree<>('d')
                )
            )
        );
    assertThat(t.descendant(List.of(1, 0)))
        .as("[1] descendant of a(b;c(d)) is d")
        .isEqualTo(new Tree<>('d'));
  }

  @Test
  void depthFirstLabels() {
    Tree<Character> t = new Tree<>(
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
    );
    assertThat(t.depthFirstLabels())
        .as("visit nodes of a(b;c(d))")
        .containsExactly('a', 'b', 'c', 'd');
  }

  @Test
  void withAt() {
    Tree<Character> tSmall = new Tree<>(
        'a',
        List.of(
            new Tree<>('b'),
            new Tree<>('c')
        )
    );
    Tree<Character> subT = new Tree<>(
        'd',
        List.of(
            new Tree<>('e'),
            new Tree<>('f')
        )
    );
    Tree<Character> tBig = new Tree<>(
        'a',
        List.of(
            new Tree<>('b'),
            new Tree<>(
                'd',
                List.of(
                    new Tree<>('e'),
                    new Tree<>('f')
                )
            )
        )
    );
    assertThat(tSmall.withAt(subT, List.of(1)))
        .as("replace leaf with subtree")
        .isEqualTo(tBig);
  }

  @Test
  void from() {
    Map<List<Integer>, Character> lineageMap = Map.ofEntries(
        Map.entry(List.of(), 'a'),
        Map.entry(List.of(0), 'b'),
        Map.entry(List.of(1), 'c'),
        Map.entry(List.of(1, 0), 'd')
    );
    Tree<Character> t = new Tree<>(
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
    );
    assertThat(Tree.from(l -> Optional.ofNullable(lineageMap.get(l))))
        .as("from lineage of a(b,c(d))")
        .isEqualTo(t);
  }

  @Test
  void leafLabels() {
    Tree<Character> t = new Tree<>(
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
    );
    assertThat(t.leafLabels())
        .as("visit labels of a(b;c(d))")
        .containsExactly('b', 'd');
  }

  @Test
  void height() {
    Tree<Character> t1 = new Tree<>('a');
    assertThat(t1.height())
        .as("height of leaf")
        .isEqualTo(1);
    Tree<Character> t3 = new Tree<>(
        'a',
        List.of(
            new Tree<>('b'),
            new Tree<>('c')
        )
    );
    assertThat(t3.height())
        .as("height of a(b;c)")
        .isEqualTo(2);
    Tree<Character> t4 = new Tree<>(
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
    );
    assertThat(t4.height())
        .as("height of a(b;c(d))")
        .isEqualTo(3);
  }

  @Test
  void isLeaf() {
    Tree<Character> t1 = new Tree<>('a');
    assertThat(t1.isLeaf())
        .as("a is leaf")
        .isTrue();
    Tree<Character> t3 = new Tree<>(
        'a',
        List.of(
            new Tree<>('b'),
            new Tree<>('c')
        )
    );
    assertThat(t3.isLeaf())
        .as("a(b;c) is not leaf")
        .isFalse();
    Tree<Character> t4 = new Tree<>(
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
    );
    assertThat(t4.isLeaf())
        .as("a(b;c(d)) is not leaf")
        .isFalse();
  }

  @Test
  void lineages() {
    Tree<Character> t = new Tree<>(
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
    );
    assertThat(t.lineages())
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
    Tree<Character> t1 = new Tree<>('a');
    assertThat(t1.size())
        .as("size of leaf")
        .isEqualTo(1);
    Tree<Character> t3 = new Tree<>(
        'a',
        List.of(
            new Tree<>('b'),
            new Tree<>('c')
        )
    );
    assertThat(t3.size())
        .as("size of a(b;c)")
        .isEqualTo(3);
    Tree<Character> t4 = new Tree<>(
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
    );
    assertThat(t4.size())
        .as("size of a(b;c(d))")
        .isEqualTo(4);
  }
}