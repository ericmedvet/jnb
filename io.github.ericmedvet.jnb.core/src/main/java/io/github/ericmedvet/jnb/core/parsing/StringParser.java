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

import io.github.ericmedvet.jnb.core.InterpolableString;
import io.github.ericmedvet.jnb.core.MapNamedParamMap;
import io.github.ericmedvet.jnb.core.NamedParamMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringParser {

  public static final String LINE_TERMINATOR_REGEX = "(\\r\\n)|(\\r)|(\\n)";
  public static final String COMMENT_REGEX = "%[^\\n\\r]*(" + LINE_TERMINATOR_REGEX + ")\\s*";
  public static final String VOID_REGEX = "\\s*(" + COMMENT_REGEX + ")*";
  public static final String CONST_NAME_PREFIX = "$";
  public static final String PLAIN_STRING_REGEX = "[A-Za-z][A-Za-z0-9_]*";
  public static final String QUOTED_STRING_REGEX = "\"[^\"]*\"";
  public static final String INTERPOLATED_STRING_REGEX = "\"\"[^\"]*\"\"";
  public static final String IMPORT_REGEX = "@import";

  private static final Logger L = Logger.getLogger(StringParser.class.getName());

  private final String s;
  private final Path path;
  private final Map<String, Node> consts;

  private StringParser(String s, Path path, Map<String, Node> consts) {
    this.s = s;
    this.path = path;
    this.consts = consts;
  }

  private static NamedParamMap from(ENode eNode) {
    Map<String, Object> values = new HashMap<>();
    eNode.child()
        .children()
        .forEach(
            n -> values.put(
                n.name(),
                switch (n.value()) {
                  case ValuedNode<?> vn -> vn.value();
                  case ENode en -> from(en);
                  case LDNode ldn -> ldn.child().children().stream().map(DNode::value).toList();
                  case LSNode lsn -> lsn.child()
                      .children()
                      .stream()
                      .map(ValuedNode::value)
                      .toList();
                  case LENode len -> len.child()
                      .children()
                      .stream()
                      .map(StringParser::from)
                      .toList();
                  default -> null;
                }
            )
        );
    return new MapNamedParamMap(eNode.name(), values);
  }

  public static NamedParamMap parse(String s) {
    return parse(s, null);
  }

  public static NamedParamMap parse(String s, Path path) {
    StringParser stringParser = new StringParser(s, path, new TreeMap<>());
    try {
      ISCSENode cseNode = stringParser.parseISCSE(0);
      TokenType.END_OF_STRING.next(s, cseNode.token().end(), path);
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
    Token tName = TokenType.CONST_NAME.next(s, i, path);
    Token tAssign = TokenType.ASSIGN_SEPARATOR.next(s, tName.end(), path);
    Node value = parseValue(tAssign.end());
    String constantName = tName.trimmedContent(s);
    Node previousValue = consts.put(constantName, value);
    if (previousValue != null) {
      L.warning(
          "Ri-definition of constant %s @ %s of %s".formatted(
              constantName,
              StringPosition.from(s, i),
              path
          )
      );
    }
    return new CNode(new Token(tName.start(), value.token().end()), constantName, value);
  }

  private Node parseConst(int i) throws ParseException {
    Token tConstName = TokenType.CONST_NAME.next(s, i, path);
    String constName = tConstName.trimmedContent(s);
    Node value = consts.get(constName);
    return switch (value) {
      case null -> throw new UndefinedConstantNameException(
          i,
          s,
          path,
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
          s,
          path
      );
    };
  }

  private DNode parseD(int i) throws ParseException {
    Token tNum = TokenType.NUM.next(s, i, path);
    try {
      return new DNode(tNum, Double.parseDouble(tNum.trimmedContent(s)));
    } catch (NumberFormatException e) {
      throw new ParseException(e.getMessage(), e, i, s, path);
    }
  }

  private ENode parseE(int i) throws ParseException {
    Token tName = TokenType.NAME.next(s, i, path);
    Token tOpenPar = TokenType.OPEN_CONTENT.next(s, tName.end(), path);
    ListNode<NPNode> npsNode = parseListNode(
        tOpenPar.end(),
        this::parseNP,
        TokenType.LIST_SEPARATOR,
        false,
        Set.of(NPNode.class)
    );
    Token tClosedPar = TokenType.CLOSED_CONTENT.next(s, npsNode.token().end(), path);
    return new ENode(new Token(tName.start(), tClosedPar.end()), npsNode, tName.trimmedContent(s));
  }

  private INode parseI(int i) throws ParseException {
    Token tImport = TokenType.IMPORT.next(s, i, path);
    Token tOpenPar = TokenType.OPEN_CONTENT.next(s, tImport.end(), path);
    Token tPath = TokenType.STRING.next(s, tOpenPar.end(), path);
    Token tClosedPar = TokenType.CLOSED_CONTENT.next(s, tPath.end(), path);
    if (path == null) {
      throw new ParseException("Cannot import content because path is null", null, i, s, path);
    }
    try {
      Path relativePath = Path.of(tPath.trimmedUnquotedContent(s));
      Path iPath = path.toAbsolutePath().getParent().resolve(relativePath);
      String content = Files.readString(iPath);
      StringParser innerParser = new StringParser(content, iPath, consts);
      ListNode<INode> isNode = innerParser.parseListNode(
          0,
          innerParser::parseI,
          null,
          false,
          Set.of(INode.class)
      );
      ListNode<CNode> csNode = innerParser.parseListNode(
          isNode.token().end(),
          innerParser::parseC,
          null,
          false,
          Set.of(CNode.class)
      );
      TokenType.END_OF_STRING.next(content, csNode.token().end(), path);
      return new INode(
          new Token(tImport.start(), tClosedPar.end()),
          tPath.trimmedUnquotedContent(s),
          content
      );
    } catch (IOException e) {
      throw new ParseException(
          "Cannot read import content at '%s'".formatted(tPath.trimmedUnquotedContent(s)),
          e,
          i,
          s,
          path
      );
    }
  }

  private ISNode parseIS(int i) throws ParseException {
    Token sToken = TokenType.INTERPOLATED_STRING.next(s, i, path);
    return new ISNode(sToken, new InterpolableString(sToken.trimmedUnquotedContent(s)));
  }

  private ISCSENode parseISCSE(int i) throws ParseException {
    List<INode> iNodes = new ArrayList<>();
    List<CNode> cNodes = new ArrayList<>();
    int j = i;
    while (true) {
      ListNode<INode> isNode = parseListNode(j, this::parseI, null, false, Set.of(INode.class));
      iNodes.addAll(isNode.children());
      j = isNode.token().end();
      ListNode<CNode> csNode = parseListNode(
          j,
          this::parseC,
          null,
          false,
          Set.of(CNode.class)
      );
      cNodes.addAll(csNode.children());
      j = csNode.token().end();
      if (isNode.children().isEmpty() && csNode.children().isEmpty()) {
        break;
      }
    }
    ENode eNode = parseE(j);
    int start = Stream.concat(iNodes.stream(), cNodes.stream())
        .mapToInt(n -> n.token().start())
        .min()
        .orElse(
            eNode.token()
                .start()
        );
    return new ISCSENode(new Token(start, eNode.token().end()), iNodes, cNodes, eNode);
  }

  private ValuedNode<?> parseConcatS(int i) throws ParseException {
    ListNode<ValuedNode<?>> ssNode = parseListNode(
        i,
        this::parseISOrS,
        TokenType.STRING_CONCAT,
        true,
        Set.of(SNode.class, ISNode.class)
    );
    BinaryOperator<Object> concatenator = (o1, o2) -> switch (o1) {
      case InterpolableString is1 -> switch (o2) {
        case InterpolableString is2 -> new InterpolableString(is1.format() + is2.format());
        case String s2 -> new InterpolableString(is1.format() + s2);
        default -> Optional.empty();
      };
      case String s1 -> switch (o2) {
        case InterpolableString is2 -> new InterpolableString(s1 + is2.format());
        case String s2 -> s1 + s2;
        default -> Optional.empty();
      };
      default -> Optional.empty();
    };
    Optional<?> opt = ssNode.children()
        .stream()
        .map(vn -> (Object) vn.value())
        .reduce(concatenator);
    if (opt.isEmpty()) {
      throw new ParseException("Empty string concatenation", null, i, s, path);
    }
    return switch (opt.get()) {
      case InterpolableString is -> new ISNode(ssNode.token(), is);
      case String string -> new SNode(ssNode.token(), string);
      default -> throw new ParseException(
          "Unexpected concatenated content of type %s in string concatenation".formatted(
              opt.get().getClass().getSimpleName()
          ),
          null,
          i,
          s,
          path
      );
    };
  }

  private ValuedNode<?> parseISOrS(int i) throws ParseException {
    List<WrongTokenException> wtes = new ArrayList<>();
    // TODO add parse constant
    // try IS
    try {
      return parseIS(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // try S
    try {
      return parseS(i);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    throw new ParseException(wtes);
  }

  private LDNode parseLD(int i) throws ParseException {
    List<WrongTokenException> wtes = new ArrayList<>();
    // case: interval
    try {
      Token openT = TokenType.OPEN_LIST.next(s, i, path);
      DNode minDNode = parseD(openT.end());
      Token sep1 = TokenType.INTERVAL_SEPARATOR.next(s, minDNode.token().end(), path);
      DNode stepDNode = parseD(sep1.end());
      Token sep2 = TokenType.INTERVAL_SEPARATOR.next(s, stepDNode.token().end(), path);
      DNode maxDNode = parseD(sep2.end());
      Token closedT = TokenType.CLOSED_LIST.next(s, maxDNode.token().end(), path);
      double min = minDNode.value();
      double step = stepDNode.value();
      double max = maxDNode.value();
      if (min > max || step <= 0) {
        throw new ParseException(
            "Cannot build list of numbers because min>max or step<=0: min=%f, max=%f, step=%f"
                .formatted(min, max, step),
            null,
            i,
            s,
            path
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
      Token openT = TokenType.OPEN_LIST.next(s, i, path);
      ListNode<DNode> dsNode = parseListNode(
          openT.end(),
          this::parseD,
          TokenType.LIST_SEPARATOR,
          true,
          Set.of(DNode.class)
      );
      Token closedT = TokenType.CLOSED_LIST.next(s, dsNode.token().end(), path);
      return new LDNode(new Token(openT.start(), closedT.end()), dsNode);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // nothing
    throw new ParseException(wtes);
  }

  private LENode parseLE(int i) throws ParseException {
    List<WrongTokenException> wtes = new ArrayList<>();
    // case: constant
    try {
      Node constNode = parseConst(i);
      if (constNode instanceof LENode leNode) {
        return leNode;
      }
      throw new ParseException(
          "Wrong constant type: %s is not a list".formatted(constNode),
          null,
          i,
          s,
          path
      );
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // case: list with join
    try {
      Token openT = TokenType.OPEN_CONTENT.next(s, i, path);
      NPNode npNode = parseNP(openT.end());
      Token closedT = TokenType.CLOSED_CONTENT.next(s, npNode.token().end(), path);
      Token jointT = TokenType.LIST_JOIN.next(s, closedT.end(), path);
      LENode outerLENode = parseLE(jointT.end());
      // do cartesian product
      List<ENode> originalENodes = outerLENode.child().children();
      List<ENode> eNodes = new ArrayList<>();
      originalENodes.forEach(
          oen -> eNodes.addAll(
              switch (npNode.value()) {
                case EnclosingListNode<?> eln -> eln.child()
                    .children()
                    .stream()
                    .map(
                        cn -> new ENode(
                            oen.token(),
                            ListNode.from(
                                oen.child().token(),
                                withAppended(
                                    oen.child().children(),
                                    new NPNode(cn.token(), npNode.name(), cn)
                                )
                            ),
                            oen.name()
                        )
                    )
                    .toList();
                default -> List.of(
                    new ENode(
                        oen.token(),
                        ListNode.from(
                            oen.child().token(),
                            withAppended(oen.child().children(), npNode)
                        ),
                        oen.name()
                    )
                );
              }
          )
      );
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
      Token multToken = TokenType.I_NUM.next(s, i, path);
      int multiplier = Integer.parseInt(multToken.trimmedContent(s));
      Token jointT = TokenType.LIST_JOIN.next(s, multToken.end(), path);
      LENode originalLENode = parseLE(jointT.end());
      // multiply
      List<ENode> eNodes = new ArrayList<>();
      for (int j = 0; j < multiplier; j++) {
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
      throw new ParseException(e.getMessage(), e, i, s, path);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // case: list with concat
    try {
      Token firstConcatT = TokenType.LIST_CONCAT.next(s, i, path);
      LENode firstLENode = parseLE(firstConcatT.end());
      Token secondConcatT = TokenType.LIST_CONCAT.next(s, firstLENode.token().end(), path);
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
      Token openT = TokenType.OPEN_LIST.next(s, i, path);
      ListNode<ENode> esNode = parseListNode(
          openT.end(),
          this::parseE,
          TokenType.LIST_SEPARATOR,
          true,
          Set.of(ENode.class)
      );
      Token closedT = TokenType.CLOSED_LIST.next(s, esNode.token().end(), path);
      return new LENode(new Token(openT.start(), closedT.end()), esNode);
    } catch (WrongTokenException wte) {
      wtes.add(wte);
    }
    // nothing
    throw new ParseException(wtes);
  }

  private LSNode parseLS(int i) throws ParseException {
    Token openT = TokenType.OPEN_LIST.next(s, i, path);
    ListNode<ValuedNode<?>> ssNode = parseListNode(
        openT.end(),
        this::parseConcatS,
        TokenType.LIST_SEPARATOR,
        true,
        Set.of(SNode.class, ISNode.class)
    );
    Token closedT = TokenType.CLOSED_LIST.next(s, ssNode.token().end(), path);
    return new LSNode(new Token(openT.start(), closedT.end()), ssNode);
  }

  private <N extends Node> ListNode<N> parseListNode(
      int i,
      NodeParser<? extends N> nodeParser,
      TokenType separatorToken,
      boolean withConstant,
      Set<Class<? extends N>> nodeClasses
  ) throws ParseException {
    List<N> children = new ArrayList<>();
    int j = i;
    Token lastSeparatorToken = null;
    while (true) {
      List<WrongTokenException> wtes = new ArrayList<>();
      N child = null;
      if (withConstant) {
        try {
          Node node = parseConst(j);
          if (nodeClasses.stream()
              .noneMatch(nodeClass -> nodeClass.isAssignableFrom(node.getClass())) && !children
                  .isEmpty()) {
            throw new ParseException(
                "Wrong const type fo %s: %s found, %s expected"
                    .formatted(
                        s.substring(
                            node.token().start(),
                            node.token().end()
                        ),
                        node.getClass().getSimpleName(),
                        nodeClasses.stream()
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining("|"))
                    ),
                null,
                node.token().start(),
                s,
                path
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
        if (Objects.isNull(separatorToken) || children.isEmpty()) {
          break;
        }
        throw new ParseException(
            "Trailing list separator %s".formatted(separatorToken.rendered()),
            null,
            lastSeparatorToken.start(),
            s,
            path
        );
      }
      j = child.token().end();
      children.add(child);
      if (Objects.nonNull(separatorToken)) {
        try {
          lastSeparatorToken = separatorToken.next(s, j, path);
          j = lastSeparatorToken.end();
        } catch (WrongTokenException wte) {
          break;
        }
      }
    }
    return ListNode.from(new Token(i, j), children);
  }

  private NPNode parseNP(int i) throws ParseException {
    Token tName = TokenType.STRING.next(s, i, path);
    Token tAssign = TokenType.ASSIGN_SEPARATOR.next(s, tName.end(), path);
    Node value = parseValue(tAssign.end());
    return new NPNode(
        new Token(tName.start(), value.token().end()),
        tName.trimmedContent(s),
        value
    );
  }

  private SNode parseS(int i) throws ParseException {
    Token sToken = TokenType.STRING.next(s, i, path);
    return new SNode(sToken, sToken.trimmedUnquotedContent(s));
  }

  private Node parseValue(int i) throws ParseException {
    List<ParseException> pes = new ArrayList<>();
    // try parse const name
    try {
      return parseConst(i);
    } catch (ParseException pe) {
      pes.add(pe);
    }
    // these order is with a purpose!
    try {
      return parseE(i);
    } catch (ParseException wte) {
      pes.add(wte);
    }
    try {
      return parseLE(i);
    } catch (ParseException pe) {
      pes.add(pe);
    }
    try {
      return parseD(i);
    } catch (ParseException pe) {
      pes.add(pe);
    }
    try {
      return parseConcatS(i);
    } catch (ParseException pe) {
      pes.add(pe);
    }
    try {
      return parseLD(i);
    } catch (ParseException pe) {
      pes.add(pe);
    }
    try {
      return parseLS(i);
    } catch (ParseException pe) {
      pes.add(pe);
    }
    throw new ParseException(pes);
  }

  @FunctionalInterface
  private interface NodeParser<N extends Node> {

    N parse(int i) throws ParseException;
  }

  record CNode(Token token, String name, Node value) implements Node {

  }

  record DNode(Token token, Double value) implements ValuedNode<Double> {

  }

  record ENode(Token token, ListNode<NPNode> child, String name) implements Node {

  }

  record INode(Token token, String path, String content) implements Node {

  }

  record ISCSENode(Token token, List<INode> iNodes, List<CNode> cNodes, ENode eNode) implements Node {

  }

  record ISNode(Token token, InterpolableString value) implements ValuedNode<InterpolableString> {

  }

  record LDNode(Token token, ListNode<DNode> child) implements EnclosingListNode<DNode> {

  }

  record LENode(Token token, ListNode<ENode> child) implements EnclosingListNode<ENode> {

  }

  record LSNode(Token token, ListNode<ValuedNode<?>> child) implements EnclosingListNode<ValuedNode<?>> {

  }

  record NPNode(Token token, String name, Node value) implements Node {

  }

  record SNode(Token token, String value) implements ValuedNode<String> {

  }
}
