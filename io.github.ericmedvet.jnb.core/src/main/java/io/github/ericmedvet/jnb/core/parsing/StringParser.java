/*-
 * ========================LICENSE_START=================================
 * jnb-core
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
package io.github.ericmedvet.jnb.core.parsing;

import io.github.ericmedvet.jnb.core.MapNamedParamMap;
import io.github.ericmedvet.jnb.core.NamedParamMap;
import io.github.ericmedvet.jnb.core.ParamMap;
import java.util.*;
import java.util.stream.Collectors;

public class StringParser {

  private static final int CONTEXT_SIZE = 10;

  record DNode(Token token, Number value) implements Node {}

  record DSNode(Token token, List<DNode> children) implements Node {}

  record ENode(Token token, NPSNode child, String name) implements Node {}

  record ESNode(Token token, List<ENode> children) implements Node {}

  record LDNode(Token token, DSNode child) implements Node {}

  record LENode(Token token, ESNode child) implements Node {}

  record LSNode(Token token, SSNode child) implements Node {}

  record NPNode(Token token, String name, Node value) implements Node {}

  record NPSNode(Token token, List<NPNode> children) implements Node {}

  record SNode(Token token, String value) implements Node {}

  record SSNode(Token token, List<SNode> children) implements Node {}

  private record Call(int i, Class<? extends Node> nodeClass, TokenType tokenType, Token token) {}

  private final String s;
  private final List<Call> calls;

  private StringParser(String s) {
    this.s = s;
    this.calls = new ArrayList<>();
  }

  public static NamedParamMap parse(String s) {
    StringParser stringParser = new StringParser(s);
    ENode eNode = stringParser.parse(0, ENode.class).orElseThrow();
    if (eNode.token.end() != s.length()) {
      int start = Math.max(0, eNode.token.end() - CONTEXT_SIZE);
      int end = Math.min(s.length(), eNode.token.end() + CONTEXT_SIZE);
      throw new IllegalArgumentException("Unexpected trailing content `%s` @%d `%s`[%d:%s]"
          .formatted(
              s.substring(eNode.token.end(), eNode.token.end() + 1),
              eNode.token.end(),
              s.substring(start, end).replaceAll("[\\n\\r]", "¶").replaceAll("\\s\\s+", "␣"),
              start,
              end));
    }
    return from(eNode);
  }

  private static NamedParamMap from(ENode eNode) {
    Map<MapNamedParamMap.TypedKey, Object> values = new HashMap<>();
    eNode.child().children().stream()
        .filter(n -> n.value() instanceof DNode)
        .forEach(n -> values.put(
            new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.DOUBLE),
            ((DNode) n.value()).value().doubleValue()));
    eNode.child().children().stream()
        .filter(n -> n.value() instanceof SNode)
        .forEach(n -> values.put(
            new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.STRING), ((SNode) n.value()).value()));
    eNode.child().children().stream()
        .filter(n -> n.value() instanceof ENode)
        .forEach(n -> values.put(
            new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.NAMED_PARAM_MAP), from((ENode) n.value())));
    eNode.child().children().stream()
        .filter(n -> n.value() instanceof LDNode)
        .forEach(n -> values.put(
            new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.DOUBLES),
            ((LDNode) n.value())
                .child.children().stream()
                    .map(c -> c.value().doubleValue())
                    .toList()));
    eNode.child().children().stream()
        .filter(n -> n.value() instanceof LSNode)
        .forEach(n -> values.put(
            new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.STRINGS),
            ((LSNode) n.value())
                .child().children().stream().map(SNode::value).toList()));
    eNode.child().children().stream()
        .filter(n -> n.value() instanceof LENode)
        .forEach(n -> values.put(
            new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.NAMED_PARAM_MAPS),
            ((LENode) n.value())
                .child().children().stream()
                    .map(StringParser::from)
                    .toList()));
    return new MapNamedParamMap(eNode.name(), values);
  }

  private static <T> List<T> withAppended(List<T> ts, T t) {
    List<T> newTs = new ArrayList<>(ts);
    newTs.add(t);
    return newTs;
  }

  private IllegalArgumentException buildException() {
    int lastErrorIndex = calls.stream()
        .filter(c -> c.token == null)
        .mapToInt(c -> c.i)
        .max()
        .orElseThrow();
    List<Call> lastErrorCalls = calls.stream()
        .filter(c -> c.token == null)
        .filter(c -> c.i == lastErrorIndex)
        .toList();
    Optional<Call> oLastSuccessfullCall = calls.stream()
        .filter(c -> c.token != null)
        .filter(c -> c.token.end() == lastErrorIndex)
        .max(Comparator.comparingInt(c -> c.token.length()));
    int start = Math.max(0, lastErrorIndex - CONTEXT_SIZE);
    int end = Math.min(lastErrorIndex + CONTEXT_SIZE, s.length());
    String msg = ("Syntax error: `%s` found instead of %s @%d in `%s`[%d:%d]")
        .formatted(
            cleanSubstring(s, lastErrorIndex, lastErrorIndex + 1),
            lastErrorCalls.stream()
                .map(c -> Objects.isNull(c.tokenType)
                    ? c.nodeClass.getSimpleName()
                    : "`%s`".formatted(c.tokenType.rendered()))
                .collect(Collectors.joining(" or ")),
            lastErrorIndex,
            cleanSubstring(s, start, end),
            start,
            end);
    if (oLastSuccessfullCall.isPresent()) {
      Call lastSuccessfullCall = oLastSuccessfullCall.get();
      msg = msg
          + "; last successful match: %s @%d `%s`[%d:%d]"
              .formatted(
                  Objects.isNull(lastSuccessfullCall.tokenType)
                      ? lastSuccessfullCall.nodeClass.getSimpleName()
                      : "`%s`".formatted(lastSuccessfullCall.tokenType.rendered()),
                  lastSuccessfullCall.i,
                  cleanSubstring(
                      s, lastSuccessfullCall.token.start(), lastSuccessfullCall.token.end()),
                  lastSuccessfullCall.token.start(),
                  lastSuccessfullCall.token.end());
    }
    return new IllegalArgumentException(msg);
  }

  private static String cleanSubstring(String s, int start, int end) {
    if (s.isEmpty()) {
      return "";
    }
    return s.substring(start, end).replaceAll("[\\n\\r]", "¶").replaceAll("\\s\\s+", "␣");
  }

  private <N extends Node> Optional<N> parse(int i, Class<N> nodeClass) {
    Optional<? extends Node> oNode;
    try {
      if (nodeClass.equals(DNode.class)) {
        oNode = parseD(i);
      } else if (nodeClass.equals(DSNode.class)) {
        oNode = parseDS(i);
      } else if (nodeClass.equals(ENode.class)) {
        oNode = parseE(i);
      } else if (nodeClass.equals(ESNode.class)) {
        oNode = parseES(i);
      } else if (nodeClass.equals(LDNode.class)) {
        oNode = parseLD(i);
      } else if (nodeClass.equals(LENode.class)) {
        oNode = parseLE(i);
      } else if (nodeClass.equals(LSNode.class)) {
        oNode = parseLS(i);
      } else if (nodeClass.equals(NPNode.class)) {
        oNode = parseNP(i);
      } else if (nodeClass.equals(NPSNode.class)) {
        oNode = parseNPS(i);
      } else if (nodeClass.equals(SNode.class)) {
        oNode = parseS(i);
      } else if (nodeClass.equals(SSNode.class)) {
        oNode = parseSS(i);
      } else {
        throw new IllegalArgumentException("Unknown node type %s".formatted(nodeClass.getSimpleName()));
      }
      if (oNode.isPresent()) {
        calls.add(new Call(i, nodeClass, null, oNode.get().token()));
        //noinspection unchecked
        return oNode.map(n -> (N) n);
      } else {
        calls.add(new Call(i, nodeClass, null, null));
        throw new IllegalArgumentException("Unexpected empty token without exception");
      }
    } catch (RuntimeException e) {
      calls.add(new Call(i, nodeClass, null, null));
      throw buildException();
    }
  }

  private Optional<Token> parse(int i, TokenType tokenType) {
    Optional<Token> oToken = tokenType.next(s, i);
    calls.add(new Call(i, null, tokenType, oToken.orElse(null)));
    return oToken;
  }

  private Optional<DNode> parseD(int i) {
    return TokenType.NUM.next(s, i).map(t -> new DNode(t, Double.parseDouble(t.trimmedContent(s))));
  }

  private Optional<DSNode> parseDS(int i) {
    List<DNode> nodes = new ArrayList<>();
    try {
      nodes.add(parse(i, DNode.class).orElseThrow());
      while (true) {
        Optional<Token> sepT = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
        if (sepT.isEmpty()) {
          break;
        }
        nodes.add(parse(sepT.get().end(), DNode.class).orElseThrow());
      }
    } catch (RuntimeException e) {
      if (nodes.isEmpty()) {
        return Optional.of(new DSNode(new Token(i, i), List.of()));
      }
      throw e;
    }
    return Optional.of(new DSNode(
        new Token(
            i,
            nodes.isEmpty()
                ? i
                : nodes.get(nodes.size() - 1).token().end()),
        nodes));
  }

  private Optional<ENode> parseE(int i) {
    Token tName = parse(i, TokenType.NAME).orElseThrow();
    Token tOpenPar = parse(tName.end(), TokenType.OPEN_CONTENT).orElseThrow();
    NPSNode npsNode = parse(tOpenPar.end(), NPSNode.class).orElseThrow();
    Token tClosedPar =
        parse(npsNode.token().end(), TokenType.CLOSED_CONTENT).orElseThrow();
    return Optional.of(new ENode(new Token(tName.start(), tClosedPar.end()), npsNode, tName.trimmedContent(s)));
  }

  private Optional<ESNode> parseES(int i) {
    List<ENode> nodes = new ArrayList<>();
    try {
      nodes.add(parse(i, ENode.class).orElseThrow());
      while (true) {
        Optional<Token> sepT = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
        if (sepT.isEmpty()) {
          break;
        }
        nodes.add(parse(sepT.get().end(), ENode.class).orElseThrow());
      }
    } catch (RuntimeException e) {
      if (nodes.isEmpty()) {
        return Optional.of(new ESNode(new Token(i, i), List.of()));
      }
      throw e;
    }
    return Optional.of(new ESNode(
        new Token(
            i,
            nodes.isEmpty()
                ? i
                : nodes.get(nodes.size() - 1).token().end()),
        nodes));
  }

  private Optional<LDNode> parseLD(int i) {
    // case: interval
    try {
      Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
      DNode minDNode = parse(openT.end(), DNode.class).orElseThrow();
      Token sep1 =
          parse(minDNode.token().end(), TokenType.INTERVAL_SEPARATOR).orElseThrow();
      DNode stepDNode = parse(sep1.end(), DNode.class).orElseThrow();
      Token sep2 =
          parse(stepDNode.token().end(), TokenType.INTERVAL_SEPARATOR).orElseThrow();
      DNode maxDNode = parse(sep2.end(), DNode.class).orElseThrow();
      Token closedT = parse(maxDNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
      double min = minDNode.value().doubleValue();
      double step = stepDNode.value().doubleValue();
      double max = maxDNode.value().doubleValue();
      if (min > max || step <= 0) {
        throw new IllegalArgumentException(
            "Cannot build list of numbers because min>max or step<=0: min=%f, max=%f, step=%f"
                .formatted(min, max, step));
      }
      List<DNode> dNodes = new ArrayList<>();
      for (double v = min; v <= max; v = v + step) {
        dNodes.add(new DNode(
            new Token(minDNode.token().start(), maxDNode.token().start()), v));
      }
      return Optional.of(new LDNode(
          new Token(openT.start(), closedT.end()),
          new DSNode(
              new Token(minDNode.token().start(), maxDNode.token().start()), dNodes)));
    } catch (RuntimeException e) {
      // ignore
    }
    // case: list of values
    try {
      Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
      DSNode dsNode = parse(openT.end(), DSNode.class).orElseThrow();
      Token closedT = parse(dsNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
      return Optional.of(new LDNode(new Token(openT.start(), closedT.end()), dsNode));
    } catch (RuntimeException e) {
      // ignore
    }
    // nothing
    throw new IllegalArgumentException("No valid case");
  }

  private Optional<LENode> parseLE(int i) {
    // case: list with join
    try {
      Token openT = parse(i, TokenType.OPEN_CONTENT).orElseThrow();
      NPNode npNode = parse(openT.end(), NPNode.class).orElseThrow();
      Token closedT =
          parse(npNode.token().end(), TokenType.CLOSED_CONTENT).orElseThrow();
      Token jointT = parse(closedT.end(), TokenType.LIST_JOIN).orElseThrow();
      LENode outerLENode = parse(jointT.end(), LENode.class).orElseThrow();
      // do cartesian product
      List<ENode> originalENodes = outerLENode.child().children();
      List<ENode> eNodes = new ArrayList<>();
      for (ENode originalENode : originalENodes) {
        if (npNode.value() instanceof DNode
            || npNode.value() instanceof SNode
            || npNode.value() instanceof ENode) {
          eNodes.add(new ENode(
              originalENode.token(),
              new NPSNode(
                  originalENode.child().token(),
                  withAppended(originalENode.child().children(), npNode)),
              originalENode.name()));
        } else {
          if (npNode.value() instanceof LDNode ldNode) {
            for (DNode dNode : ldNode.child().children()) {
              eNodes.add(new ENode(
                  originalENode.token(),
                  new NPSNode(
                      originalENode.child().token(),
                      withAppended(
                          originalENode.child().children(),
                          new NPNode(ldNode.token(), npNode.name(), dNode))),
                  originalENode.name()));
            }
          } else if (npNode.value() instanceof LSNode lsNode) {
            for (SNode sNode : lsNode.child().children()) {
              eNodes.add(new ENode(
                  originalENode.token(),
                  new NPSNode(
                      originalENode.child().token(),
                      withAppended(
                          originalENode.child().children(),
                          new NPNode(lsNode.token(), npNode.name(), sNode))),
                  originalENode.name()));
            }
          } else if (npNode.value() instanceof LENode leNode) {
            for (ENode eNode : leNode.child().children()) {
              eNodes.add(new ENode(
                  originalENode.token(),
                  new NPSNode(
                      originalENode.child().token(),
                      withAppended(
                          originalENode.child().children(),
                          new NPNode(leNode.token(), npNode.name(), eNode))),
                  originalENode.name()));
            }
          }
        }
      }
      return Optional.of(new LENode(
          new Token(openT.start(), outerLENode.token().end()),
          new ESNode(
              new Token(
                  npNode.token().start(), outerLENode.token().end()),
              eNodes)));
    } catch (RuntimeException e) {
      // ignore
    }
    // case: list with mult
    try {
      Token multToken = parse(i, TokenType.I_NUM).orElseThrow();
      int mult = Integer.parseInt(multToken.trimmedContent(s));
      Token jointT = parse(multToken.end(), TokenType.LIST_JOIN).orElseThrow();
      LENode originalLENode = parse(jointT.end(), LENode.class).orElseThrow();
      // multiply
      List<ENode> eNodes = new ArrayList<>();
      for (int j = 0; j < mult; j++) {
        eNodes.addAll(originalLENode.child().children());
      }
      return Optional.of(new LENode(
          new Token(multToken.start(), originalLENode.token().end()),
          new ESNode(
              new Token(
                  originalLENode.token().start(),
                  originalLENode.token().end()),
              eNodes)));
    } catch (RuntimeException e) {
      // ignore
    }
    // case: list with concat
    try {
      Token firstConcatT = parse(i, TokenType.LIST_CONCAT).orElseThrow();
      LENode firstLENode = parse(firstConcatT.end(), LENode.class).orElseThrow();
      Token secondConcatT =
          parse(firstLENode.token().end(), TokenType.LIST_CONCAT).orElseThrow();
      LENode secondLENode = parse(secondConcatT.end(), LENode.class).orElseThrow();
      // concat
      List<ENode> eNodes = new ArrayList<>(firstLENode.child().children());
      eNodes.addAll(secondLENode.child().children());
      return Optional.of(new LENode(
          new Token(firstConcatT.start(), secondLENode.token().end()),
          new ESNode(
              new Token(firstConcatT.start(), secondLENode.token().end()), eNodes)));
    } catch (RuntimeException e) {
      // ignore
    }
    // case: just list
    try {
      Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
      ESNode sNode = parse(openT.end(), ESNode.class).orElseThrow();
      Token closedT = parse(sNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
      return Optional.of(new LENode(new Token(openT.start(), closedT.end()), sNode));
    } catch (RuntimeException e) {
      // ignore
    }
    // nothing
    throw new IllegalArgumentException("No valid case");
  }

  private Optional<LSNode> parseLS(int i) {
    Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
    SSNode ssNode = parse(openT.end(), SSNode.class).orElseThrow();
    Token closedT = parse(ssNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
    return Optional.of(new LSNode(new Token(openT.start(), closedT.end()), ssNode));
  }

  private Optional<NPNode> parseNP(int i) {
    Token tName = parse(i, TokenType.STRING).orElseThrow();
    Token tAssign = parse(tName.end(), TokenType.ASSIGN_SEPARATOR).orElseThrow();
    Optional<? extends Node> oNode = Optional.empty();
    for (Class<? extends Node> nodeClass :
        List.of(ENode.class, LENode.class, DNode.class, SNode.class, LDNode.class, LSNode.class)) {
      try {
        oNode = parse(tAssign.end(), nodeClass);
        break;
      } catch (RuntimeException e) {
        // ignore
      }
    }
    Node value = oNode.orElseThrow();
    return Optional.of(new NPNode(new Token(tName.start(), value.token().end()), tName.trimmedContent(s), value));
  }

  private Optional<NPSNode> parseNPS(int i) {
    List<NPNode> nodes = new ArrayList<>();
    try {
      nodes.add(parse(i, NPNode.class).orElseThrow());
      while (true) {
        Optional<Token> ot = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
        if (ot.isEmpty()) {
          break;
        }
        nodes.add(parse(ot.get().end(), NPNode.class).orElseThrow());
      }
    } catch (RuntimeException e) {
      if (nodes.isEmpty()) {
        return Optional.of(new NPSNode(new Token(i, i), List.of()));
      }
      throw e;
    }
    return Optional.of(new NPSNode(
        new Token(
            i,
            nodes.isEmpty()
                ? i
                : nodes.get(nodes.size() - 1).token().end()),
        nodes));
  }

  private Optional<SNode> parseS(int i) {
    return parse(i, TokenType.STRING).map(t -> new SNode(t, t.trimmedUnquotedContent(s)));
  }

  private Optional<SSNode> parseSS(int i) {
    List<SNode> nodes = new ArrayList<>();
    try {
      nodes.add(parse(i, SNode.class).orElseThrow());
      while (true) {
        Optional<Token> sepT = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
        if (sepT.isEmpty()) {
          break;
        }
        nodes.add(parse(sepT.get().end(), SNode.class).orElseThrow());
      }
    } catch (RuntimeException e) {
      if (nodes.isEmpty()) {
        return Optional.of(new SSNode(new Token(i, i), List.of()));
      }
      throw e;
    }
    return Optional.of(new SSNode(
        new Token(
            i,
            nodes.isEmpty()
                ? i
                : nodes.get(nodes.size() - 1).token().end()),
        nodes));
  }
}
