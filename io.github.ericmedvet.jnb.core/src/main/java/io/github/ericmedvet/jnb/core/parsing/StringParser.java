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
import java.util.stream.Stream;

public class StringParser {

  public static final String LINE_TERMINATOR_REGEX = "(\\r\\n)|(\\r)|(\\n)";
  public static final String COMMENT_REGEX = "%[^\\n\\r]*(" + LINE_TERMINATOR_REGEX + ")\\s*";
  public static final String VOID_REGEX = "\\s*(" + COMMENT_REGEX + ")*";
  public static final String CONST_NAME_PREFIX = "$";
  public static final String PLAIN_STRING_REGEX = "[A-Za-z][A-Za-z0-9_]*";
  public static final String QUOTED_STRING_REGEX = "\"[^\"]*\"";

  private final String s;
  private final Map<String, Node> consts;

  private StringParser(String s) {
    this.s = s;
    this.consts = new TreeMap<>();
  }

  @FunctionalInterface
  private interface NodeParser<N extends Node> {

    N parse(int i) throws ParseException;
  }

  record CNode(Token token, String name, Node value) implements Node {}

  record CSENode(Token token, ListNode<CNode> csNode, ENode eNode) implements Node {}

  record DNode(Token token, Number value) implements Node {}

  record ENode(Token token, ListNode<NPNode> child, String name) implements Node {}

  record LDNode(Token token, ListNode<DNode> child) implements Node {}

  record LENode(Token token, ListNode<ENode> child) implements Node {}

  record LSNode(Token token, ListNode<SNode> child) implements Node {}

  record NPNode(Token token, String name, Node value) implements Node {}

  record SNode(Token token, String value) implements Node {}

  private static NamedParamMap from(ENode eNode) {
    Map<MapNamedParamMap.TypedKey, Object> values = new HashMap<>();
    eNode.child()
        .children()
        .stream()
        .filter(n -> n.value() instanceof DNode)
        .forEach(
            n -> values.put(
                new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.DOUBLE),
                ((DNode) n.value()).value().doubleValue()
            )
        );
    eNode.child()
        .children()
        .stream()
        .filter(n -> n.value() instanceof SNode)
        .forEach(
            n -> values.put(
                new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.STRING),
                ((SNode) n.value()).value()
            )
        );
    eNode.child()
        .children()
        .stream()
        .filter(n -> n.value() instanceof ENode)
        .forEach(
            n -> values.put(
                new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.NAMED_PARAM_MAP),
                from((ENode) n.value())
            )
        );
    eNode.child()
        .children()
        .stream()
        .filter(n -> n.value() instanceof LDNode)
        .forEach(
            n -> values.put(
                new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.DOUBLES),
                ((LDNode) n.value()).child.children()
                    .stream()
                    .map(c -> c.value().doubleValue())
                    .toList()
            )
        );
    eNode.child()
        .children()
        .stream()
        .filter(n -> n.value() instanceof LSNode)
        .forEach(
            n -> values.put(
                new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.STRINGS),
                ((LSNode) n.value())
                    .child()
                    .children()
                    .stream()
                    .map(SNode::value)
                    .toList()
            )
        );
    eNode.child()
        .children()
        .stream()
        .filter(n -> n.value() instanceof LENode)
        .forEach(
            n -> values.put(
                new MapNamedParamMap.TypedKey(n.name, ParamMap.Type.NAMED_PARAM_MAPS),
                ((LENode) n.value())
                    .child()
                    .children()
                    .stream()
                    .map(StringParser::from)
                    .toList()
            )
        );
    return new MapNamedParamMap(eNode.name(), values);
  }

  public static NamedParamMap parse(String s) {
    StringParser stringParser = new StringParser(s);
    try {
      CSENode cseNode = stringParser.parseCSE(0);
      TokenType.END_OF_STRING.next(s, cseNode.token().end());
      ENode eNode = cseNode.eNode();
      return from(eNode);
    } catch (ParseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static <T> List<T> withAppended(List<T> ts, T t) {
    List<T> newTs = new ArrayList<>(ts);
    newTs.add(t);
    return newTs;
  }

  private CNode parseC(int i) throws ParseException {
    Token tName = TokenType.CONST_NAME.next(s, i);
    Token tAssign = TokenType.ASSIGN_SEPARATOR.next(s, tName.end());
    Node value = parseValue(tAssign.end());
    String constantName = tName.trimmedContent(s);
    consts.put(constantName, value);
    return new CNode(new Token(tName.start(), value.token().end()), constantName, value);
  }

  private CSENode parseCSE(int i) throws ParseException {
    ListNode<CNode> csNode = parseListNode(i, this::parseC, CNode.class, false, false);
    ENode eNode = parseE(csNode.token().end());
    return new CSENode(new Token(csNode.token().start(), eNode.token().end()), csNode, eNode);
  }

  private DNode parseD(int i) throws ParseException {
    Token tNum = TokenType.NUM.next(s, i);
    try {
      return new DNode(tNum, Double.parseDouble(tNum.trimmedContent(s)));
    } catch (NumberFormatException e) {
      throw new ParseException(e.getMessage(), e, i, s);
    }
  }

  private ENode parseE(int i) throws ParseException {
    Token tName = TokenType.NAME.next(s, i);
    Token tOpenPar = TokenType.OPEN_CONTENT.next(s, tName.end());
    ListNode<NPNode> npsNode = parseListNode(tOpenPar.end(), this::parseNP, NPNode.class, true, false);
    Token tClosedPar = TokenType.CLOSED_CONTENT.next(s, npsNode.token().end());
    return new ENode(new Token(tName.start(), tClosedPar.end()), npsNode, tName.trimmedContent(s));
  }

  private LDNode parseLD(int i) throws ParseException {
    List<WrongTokenException> wtes = new ArrayList<>();
    // case: interval
    try {
      Token openT = TokenType.OPEN_LIST.next(s, i);
      DNode minDNode = parseD(openT.end());
      Token sep1 = TokenType.INTERVAL_SEPARATOR.next(s, minDNode.token().end());
      DNode stepDNode = parseD(sep1.end());
      Token sep2 = TokenType.INTERVAL_SEPARATOR.next(s, stepDNode.token().end());
      DNode maxDNode = parseD(sep2.end());
      Token closedT = TokenType.CLOSED_LIST.next(s, maxDNode.token().end());
      double min = minDNode.value().doubleValue();
      double step = stepDNode.value().doubleValue();
      double max = maxDNode.value().doubleValue();
      if (min > max || step <= 0) {
        throw new ParseException(
            "Cannot build list of numbers because min>max or step<=0: min=%f, max=%f, step=%f"
                .formatted(min, max, step),
            null,
            i,
            s
        );
      }
      List<DNode> dNodes = new ArrayList<>();
      for (double v = min; v <= max; v = v + step) {
        dNodes.add(
            new DNode(
                new Token(minDNode.token().start(), maxDNode.token().start()),
                v
            )
        );
      }
      return new LDNode(
          new Token(openT.start(), closedT.end()),
          ListNode.from(
              new Token(minDNode.token().start(), maxDNode.token().start()),
              dNodes
          )
      );
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // case: list of values
    try {
      Token openT = TokenType.OPEN_LIST.next(s, i);
      ListNode<DNode> dsNode = parseListNode(openT.end(), this::parseD, DNode.class, true, true);
      Token closedT = TokenType.CLOSED_LIST.next(s, dsNode.token().end());
      return new LDNode(new Token(openT.start(), closedT.end()), dsNode);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // nothing
    throw new CompositeWrongTokenException(wtes);
  }

  @SuppressWarnings("InfiniteRecursion")
  private LENode parseLE(int i) throws ParseException {
    List<WrongTokenException> wtes = new ArrayList<>();
    // case: list with join
    try {
      Token openT = TokenType.OPEN_CONTENT.next(s, i);
      NPNode npNode = parseNP(openT.end());
      Token closedT = TokenType.CLOSED_CONTENT.next(s, npNode.token().end());
      Token jointT = TokenType.LIST_JOIN.next(s, closedT.end());
      LENode outerLENode = parseLE(jointT.end());
      // do cartesian product
      List<ENode> originalENodes = outerLENode.child().children();
      List<ENode> eNodes = new ArrayList<>();
      for (ENode originalENode : originalENodes) {
        if (npNode.value() instanceof DNode || npNode.value() instanceof SNode || npNode.value() instanceof ENode) {
          eNodes.add(
              new ENode(
                  originalENode.token(),
                  ListNode.from(
                      originalENode.child().token(),
                      withAppended(originalENode.child().children(), npNode)
                  ),
                  originalENode.name()
              )
          );
        } else {
          if (npNode.value() instanceof LDNode(Token token,ListNode<DNode>child)) {
            for (DNode dNode : child.children()) {
              eNodes.add(
                  new ENode(
                      originalENode.token(),
                      ListNode.from(
                          originalENode.child().token(),
                          withAppended(
                              originalENode.child().children(),
                              new NPNode(token, npNode.name(), dNode)
                          )
                      ),
                      originalENode.name()
                  )
              );
            }
          } else if (npNode.value() instanceof LSNode(Token token,ListNode<SNode>child)) {
            for (SNode sNode : child.children()) {
              eNodes.add(
                  new ENode(
                      originalENode.token(),
                      ListNode.from(
                          originalENode.child().token(),
                          withAppended(
                              originalENode.child().children(),
                              new NPNode(token, npNode.name(), sNode)
                          )
                      ),
                      originalENode.name()
                  )
              );
            }
          } else if (npNode.value() instanceof LENode(Token token,ListNode<ENode>child)) {
            for (ENode eNode : child.children()) {
              eNodes.add(
                  new ENode(
                      originalENode.token(),
                      ListNode.from(
                          originalENode.child().token(),
                          withAppended(
                              originalENode.child().children(),
                              new NPNode(token, npNode.name(), eNode)
                          )
                      ),
                      originalENode.name()
                  )
              );
            }
          }
        }
      }
      return new LENode(
          new Token(openT.start(), outerLENode.token().end()),
          ListNode.from(
              new Token(
                  npNode.token().start(),
                  outerLENode.token().end()
              ),
              eNodes
          )
      );
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // case: list with mult
    try {
      Token multToken = TokenType.I_NUM.next(s, i);
      int mult = Integer.parseInt(multToken.trimmedContent(s));
      Token jointT = TokenType.LIST_JOIN.next(s, multToken.end());
      LENode originalLENode = parseLE(jointT.end());
      // multiply
      List<ENode> eNodes = new ArrayList<>();
      for (int j = 0; j < mult; j++) {
        eNodes.addAll(originalLENode.child().children());
      }
      return new LENode(
          new Token(multToken.start(), originalLENode.token().end()),
          ListNode.from(
              new Token(
                  originalLENode.token().start(),
                  originalLENode.token().end()
              ),
              eNodes
          )
      );
    } catch (NumberFormatException e) {
      throw new ParseException(e.getMessage(), e, i, s);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // case: list with concat
    try {
      Token firstConcatT = TokenType.LIST_CONCAT.next(s, i);
      LENode firstLENode = parseLE(firstConcatT.end());
      Token secondConcatT = TokenType.LIST_CONCAT.next(s, firstLENode.token().end());
      LENode secondLENode = parseLE(secondConcatT.end());
      // concat
      return new LENode(
          new Token(firstConcatT.start(), secondLENode.token().end()),
          ListNode.from(
              new Token(firstConcatT.start(), secondLENode.token().end()),
              Stream.concat(
                  firstLENode.child().children().stream(),
                  secondLENode.child().children().stream()
              )
                  .toList()
          )
      );
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // case: just list
    try {
      Token openT = TokenType.OPEN_LIST.next(s, i);
      ListNode<ENode> esNode = parseListNode(openT.end(), this::parseE, ENode.class, true, true);
      Token closedT = TokenType.CLOSED_LIST.next(s, esNode.token().end());
      return new LENode(new Token(openT.start(), closedT.end()), esNode);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // nothing
    throw new CompositeWrongTokenException(wtes);
  }

  private LSNode parseLS(int i) throws ParseException {
    Token openT = TokenType.OPEN_LIST.next(s, i);
    ListNode<SNode> ssNode = parseListNode(openT.end(), this::parseS, SNode.class, true, true);
    Token closedT = TokenType.CLOSED_LIST.next(s, ssNode.token().end());
    return new LSNode(new Token(openT.start(), closedT.end()), ssNode);
  }

  private <N extends Node> ListNode<N> parseListNode(
      int i,
      NodeParser<N> nodeParser,
      Class<N> nodeClass,
      boolean withSeparator,
      boolean withConstant
  ) throws ParseException {
    List<N> children = new ArrayList<>();
    int j = i;
    while (true) {
      List<WrongTokenException> wtes = new ArrayList<>();
      N child = null;
      if (withConstant) {
        try {
          Node node = parseConst(j);
          if (!nodeClass.isAssignableFrom(node.getClass()) && !children.isEmpty()) {
            throw new ParseException(
                "Wrong const type fo %s: %s found, %s expected"
                    .formatted(
                        s.substring(
                            node.token().start(),
                            node.token().end()
                        ),
                        node.getClass().getSimpleName(),
                        nodeClass.getSimpleName()
                    ),
                null,
                node.token().start(),
                s
            );
          }
          //noinspection unchecked
          child = (N) node;
        } catch (WrongTokenException wte) {
          wtes.add(wte);
        }
      }
      try {
        child = nodeParser.parse(j);
      } catch (WrongTokenException wte) {
        wtes.add(wte);
      }
      if (child == null) {
        if (!withSeparator || children.isEmpty()) {
          break;
        }
        throw new CompositeWrongTokenException(wtes);
      }
      j = child.token().end();
      children.add(child);
      if (withSeparator) {
        try {
          j = TokenType.LIST_SEPARATOR.next(s, j).end();
        } catch (WrongTokenException wte) {
          break;
        }
      }
    }
    return ListNode.from(new Token(i, j), children);
  }

  private NPNode parseNP(int i) throws ParseException {
    Token tName = TokenType.STRING.next(s, i);
    Token tAssign = TokenType.ASSIGN_SEPARATOR.next(s, tName.end());
    Node value = parseValue(tAssign.end());
    return new NPNode(new Token(tName.start(), value.token().end()), tName.trimmedContent(s), value);
  }

  private SNode parseS(int i) throws ParseException {
    Token sToken = TokenType.STRING.next(s, i);
    return new SNode(sToken, sToken.trimmedUnquotedContent(s));
  }

  private Node parseConst(int i) throws ParseException {
    Token tConstName = TokenType.CONST_NAME.next(s, i);
    String constName = tConstName.trimmedContent(s);
    Node value = consts.get(constName);
    return switch (value) {
      case null -> throw new UndefinedConstantNameException(
          i,
          s,
          constName,
          consts.keySet().stream().toList()
      );
      case DNode dNode -> new DNode(tConstName, dNode.value());
      case ENode eNode -> new ENode(tConstName, eNode.child(), eNode.name());
      case SNode sNode -> new SNode(tConstName, sNode.value());
      case LDNode ldNode -> new LDNode(tConstName, ldNode.child());
      case LENode leNode -> new LENode(tConstName, leNode.child());
      case LSNode lsNode -> new LSNode(tConstName, lsNode.child());
      default -> throw new ParseException(
          "Unknown type %s of const %s".formatted(value.getClass().getSimpleName(), constName),
          null,
          i,
          s
      );
    };
  }

  private Node parseValue(int i) throws ParseException {
    List<WrongTokenException> wtes = new ArrayList<>();
    // try parse const name
    try {
      return parseConst(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // these order is with a purpose!
    try {
      return parseE(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    try {
      return parseLE(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    try {
      return parseD(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    try {
      return parseS(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    try {
      return parseLD(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    try {
      return parseLS(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    throw new CompositeWrongTokenException(wtes);
  }
}
