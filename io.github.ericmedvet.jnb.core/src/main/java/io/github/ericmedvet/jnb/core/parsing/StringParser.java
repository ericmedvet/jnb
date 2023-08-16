package io.github.ericmedvet.jnb.core.parsing;

import io.github.ericmedvet.jnb.core.MapNamedParamMap;
import io.github.ericmedvet.jnb.core.NamedParamMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StringParser {

  private StringParser() {
  }

  private record DNode(Token token, Number value) implements Node {
    static Parser<DNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> TokenType.NUM.next(s, i).map(t -> new DNode(
          t,
          Double.parseDouble(t.trimmedContent(s))
      )).orElseThrow(tokenPE(TokenType.NUM, i, s, parentNodeClass, parentIndex));
    }
  }

  private record DSNode(Token token, List<DNode> children) implements Node {
    static Parser<DSNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        List<DNode> nodes = new ArrayList<>();
        try {
          nodes.add(DNode.parser().parse(s, i, DSNode.class, i));
        } catch (ParsingException e) {
          //ignore
        }
        while (!nodes.isEmpty()) {
          Optional<Token> sepT = TokenType.LIST_SEPARATOR.next(s, nodes.get(nodes.size() - 1).token().end());
          if (sepT.isEmpty()) {
            break;
          }
          nodes.add(DNode.parser().parse(s, sepT.get().end(), DSNode.class, i));
        }
        return new DSNode(new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()), nodes);
      };
    }
  }

  private record ENode(Token token, NPSNode child, String name) implements Node {
    static Parser<ENode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        Token tName = TokenType.NAME.next(s, i).orElseThrow(tokenPE(
            TokenType.NAME,
            i,
            s,
            parentNodeClass,
            parentIndex
        ));
        Token tOpenPar = TokenType.OPEN_CONTENT.next(s, tName.end()).orElseThrow(tokenPE(
            TokenType.OPEN_CONTENT,
            tName.end(),
            s,
            parentNodeClass,
            parentIndex
        ));
        NPSNode npsNode = NPSNode.parser().parse(s, tOpenPar.end(), ENode.class, i);
        Token tClosedPar = TokenType.CLOSED_CONTENT.next(
            s,
            npsNode.token().end()
        ).orElseThrow(tokenPE(
            TokenType.CLOSED_CONTENT,
            npsNode.token().end(),
            s,
            parentNodeClass,
            parentIndex
        ));
        return new ENode(new Token(tName.start(), tClosedPar.end()), npsNode, tName.trimmedContent(s));
      };
    }
  }

  private record ESNode(Token token, List<ENode> children) implements Node {
    static Parser<ESNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        List<ENode> nodes = new ArrayList<>();
        try {
          nodes.add(ENode.parser().parse(s, i, ESNode.class, i));
        } catch (ParsingException e) {
          //ignore
        }
        while (!nodes.isEmpty()) {
          Optional<Token> sepT = TokenType.LIST_SEPARATOR.next(s, nodes.get(nodes.size() - 1).token().end());
          if (sepT.isEmpty()) {
            break;
          }
          nodes.add(ENode.parser().parse(s, sepT.get().end(), ESNode.class, i));
        }
        return new ESNode(new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()), nodes);
      };
    }
  }

  private record LDNode(Token token, DSNode child) implements Node {
    static Parser<LDNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        try {
          Token openT = TokenType.OPEN_LIST.next(s, i).orElseThrow(tokenPE(
              TokenType.OPEN_LIST,
              i,
              s,
              parentNodeClass,
              parentIndex
          ));
          DNode minDNode = DNode.parser().parse(s, openT.end(), LDNode.class, i);
          Token sep1 = TokenType.INTERVAL_SEPARATOR.next(s, minDNode.token().end()).orElseThrow(tokenPE(
              TokenType.INTERVAL_SEPARATOR,
              minDNode.token().end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          DNode stepDNode = DNode.parser().parse(s, sep1.end(), LDNode.class, i);
          Token sep2 = TokenType.INTERVAL_SEPARATOR.next(s, stepDNode.token().end()).orElseThrow(tokenPE(
              TokenType.INTERVAL_SEPARATOR,
              stepDNode.token().end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          DNode maxDNode = DNode.parser().parse(s, sep2.end(), LDNode.class, i);
          Token closedT = TokenType.CLOSED_LIST.next(s, maxDNode.token().end()).orElseThrow(tokenPE(
              TokenType.CLOSED_LIST,
              maxDNode.token().end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          double min = minDNode.value().doubleValue();
          double step = stepDNode.value().doubleValue();
          double max = maxDNode.value().doubleValue();
          if (min > max || step <= 0) {
            throw new IllegalArgumentException(
                "Cannot build list of numbers because min>max or step<=0: min=%f, max=%f, step=%f".formatted(
                    min,
                    max,
                    step
                ));
          }
          List<DNode> dNodes = new ArrayList<>();
          for (double v = min; v <= max; v = v + step) {
            dNodes.add(new DNode(new Token(minDNode.token().start(), maxDNode.token().start()), v));
          }
          return new LDNode(
              new Token(openT.start(), closedT.end()),
              new DSNode(
                  new Token(minDNode.token().start(), maxDNode.token().start()),
                  dNodes
              )
          );
        } catch (ParsingException e) {
          //ignore
        }
        try {
          Token openT = TokenType.OPEN_LIST.next(s, i).orElseThrow(tokenPE(
              TokenType.OPEN_LIST,
              i,
              s,
              parentNodeClass,
              parentIndex
          ));
          DSNode dsNode = DSNode.parser().parse(s, openT.end(), LDNode.class, i);
          Token closedT = TokenType.CLOSED_LIST.next(s, dsNode.token().end()).orElseThrow(tokenPE(
              TokenType.CLOSED_LIST,
              dsNode.token().end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          return new LDNode(new Token(openT.start(), closedT.end()), dsNode);
        } catch (ParsingException e) {
          //ignore
        }
        throw tokenPE(
            TokenType.OPEN_LIST,
            i,
            s,
            parentNodeClass,
            parentIndex
        ).get();
      };
    }
  }

  private record LENode(Token token, ESNode child) implements Node {
    static Parser<LENode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        //list with join
        try {
          Token openT = TokenType.OPEN_CONTENT.next(s, i).orElseThrow(tokenPE(
              TokenType.OPEN_CONTENT,
              i,
              s,
              parentNodeClass,
              parentIndex
          ));
          NPNode npNode = NPNode.parser().parse(s, openT.end(), LENode.class, i);
          Token closedT = TokenType.CLOSED_CONTENT.next(s, npNode.token().end()).orElseThrow(tokenPE(
              TokenType.CLOSED_CONTENT,
              npNode.token().end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          Token jointT = TokenType.LIST_JOIN.next(s, closedT.end()).orElseThrow(tokenPE(
              TokenType.LIST_JOIN,
              closedT.end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          LENode outerLENode = LENode.parser().parse(s, jointT.end(), LENode.class, i);
          //do cartesian product
          List<ENode> originalENodes = outerLENode.child().children();
          List<ENode> eNodes = new ArrayList<>();
          for (ENode originalENode : originalENodes) {
            if (npNode.value() instanceof DNode || npNode.value() instanceof SNode || npNode.value() instanceof ENode) {
              eNodes.add(new ENode(
                  originalENode.token(),
                  new NPSNode(originalENode.child().token(), withAppended(originalENode.child().children(), npNode)),
                  originalENode.name()
              ));
            } else {
              if (npNode.value() instanceof LDNode ldNode) {
                for (DNode dNode : ldNode.child().children()) {
                  eNodes.add(new ENode(
                      originalENode.token(),
                      new NPSNode(originalENode.child().token(), withAppended(
                          originalENode.child().children(),
                          new NPNode(ldNode.token(), npNode.name(), dNode)
                      )),
                      originalENode.name()
                  ));
                }
              } else if (npNode.value() instanceof LSNode lsNode) {
                for (SNode sNode : lsNode.child().children()) {
                  eNodes.add(new ENode(
                      originalENode.token(),
                      new NPSNode(originalENode.child().token(), withAppended(
                          originalENode.child().children(),
                          new NPNode(lsNode.token(), npNode.name(), sNode)
                      )),
                      originalENode.name()
                  ));
                }
              } else if (npNode.value() instanceof LENode leNode) {
                for (ENode eNode : leNode.child().children()) {
                  eNodes.add(new ENode(
                      originalENode.token(),
                      new NPSNode(originalENode.child().token(), withAppended(
                          originalENode.child().children(),
                          new NPNode(leNode.token(), npNode.name(), eNode)
                      )),
                      originalENode.name()
                  ));
                }
              }
            }
          }
          return new LENode(new Token(openT.start(), outerLENode.token().end()), new ESNode(
              new Token(npNode.token().start(), outerLENode.token.end()),
              eNodes
          ));
        } catch (ParsingException e) {
          //ignore
        }
        //list with mult
        try {
          Token multToken = TokenType.I_NUM.next(s, i).orElseThrow(tokenPE(
              TokenType.I_NUM,
              i,
              s,
              parentNodeClass,
              parentIndex
          ));
          int mult = Integer.parseInt(multToken.trimmedContent(s));
          Token jointT = TokenType.LIST_JOIN.next(s, multToken.end()).orElseThrow(tokenPE(
              TokenType.LIST_JOIN,
              multToken.end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          LENode originalLENode = LENode.parser().parse(s, jointT.end(), LENode.class, i);
          //multiply
          List<ENode> eNodes = new ArrayList<>();
          for (int j = 0; j < mult; j++) {
            eNodes.addAll(originalLENode.child().children());
          }
          return new LENode(new Token(multToken.start(), originalLENode.token().end()), new ESNode(
              new Token(originalLENode.token().start(), originalLENode.token().end()),
              eNodes
          ));
        } catch (ParsingException e) {
          //ignore
        }
        //list with concat
        try {
          Token firstConcatT = TokenType.LIST_CONCAT.next(s, i).orElseThrow(tokenPE(
              TokenType.LIST_CONCAT,
              i,
              s,
              parentNodeClass,
              parentIndex
          ));
          LENode firstLENode = LENode.parser().parse(s, firstConcatT.end(), LENode.class, i);
          Token secondConcatT = TokenType.LIST_CONCAT.next(s, firstLENode.token().end()).orElseThrow(tokenPE(
              TokenType.LIST_CONCAT,
              firstLENode.token().end(),
              s,
              parentNodeClass,
              parentIndex
          ));
          LENode secondLENode = LENode.parser().parse(s, secondConcatT.end(), LENode.class, i);
          //concat
          List<ENode> eNodes = new ArrayList<>(firstLENode.child().children());
          eNodes.addAll(secondLENode.child().children());
          return new LENode(new Token(firstConcatT.start(), secondLENode.token().end()), new ESNode(
              new Token(firstConcatT.start(), secondLENode.token().end()),
              eNodes
          ));
        } catch (ParsingException e) {
          //ignore
        }
        //just list
        Token openT = TokenType.OPEN_LIST.next(s, i).orElseThrow(tokenPE(
            TokenType.OPEN_LIST,
            i,
            s,
            parentNodeClass,
            parentIndex
        ));
        ESNode sNode = ESNode.parser().parse(s, openT.end(), LENode.class, i);
        Token closedT = TokenType.CLOSED_LIST.next(s, sNode.token().end()).orElseThrow(tokenPE(
            TokenType.CLOSED_LIST,
            sNode.token().end(),
            s,
            parentNodeClass,
            parentIndex
        ));
        return new LENode(new Token(openT.start(), closedT.end()), sNode);
      };
    }
  }

  private record LSNode(Token token, SSNode child) implements Node {
    static Parser<LSNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        Token openT = TokenType.OPEN_LIST.next(s, i).orElseThrow(tokenPE(
            TokenType.OPEN_LIST,
            i,
            s,
            parentNodeClass,
            parentIndex
        ));
        SSNode sNode = SSNode.parser().parse(s, openT.end(), LSNode.class, i);
        Token closedT = TokenType.CLOSED_LIST.next(s, sNode.token().end()).orElseThrow(tokenPE(
            TokenType.CLOSED_LIST,
            sNode.token().end(),
            s,
            parentNodeClass,
            parentIndex
        ));
        return new LSNode(new Token(openT.start(), closedT.end()), sNode);
      };
    }
  }

  private record NPNode(Token token, String name, Node value) implements Node {
    static Parser<NPNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        Token tName = TokenType.STRING.next(s, i).orElseThrow(tokenPE(
            TokenType.STRING,
            i,
            s,
            parentNodeClass,
            parentIndex
        ));
        Token tAssign = TokenType.ASSIGN_SEPARATOR.next(s, tName.end()).orElseThrow(tokenPE(
            TokenType.ASSIGN_SEPARATOR,
            tName.end(),
            s,
            parentNodeClass,
            parentIndex
        ));
        Node value = null;
        try {
          value = ENode.parser().parse(s, tAssign.end(), NPNode.class, i);
        } catch (ParsingException e) {
          //ignore
        }
        if (value == null) {
          try {
            value = LENode.parser().parse(s, tAssign.end(), NPNode.class, i);
          } catch (ParsingException e) {
            //ignore
          }
        }
        if (value == null) {
          try {
            value = DNode.parser().parse(s, tAssign.end(), NPNode.class, i);
          } catch (ParsingException e) {
            //ignore
          }
        }
        if (value == null) {
          try {
            value = SNode.parser().parse(s, tAssign.end(), NPNode.class, i);
          } catch (ParsingException e) {
            //ignore
          }
        }
        if (value == null) {
          try {
            value = LDNode.parser().parse(s, tAssign.end(), NPNode.class, i);
          } catch (ParsingException e) {
            //ignore
          }
        }
        if (value == null) {
          try {
            value = LSNode.parser().parse(s, tAssign.end(), NPNode.class, i);
          } catch (ParsingException e) {
            //ignore
          }
        }
        if (value == null) {
          throw tokenPE(
              null,
              i,
              s,
              parentNodeClass,
              parentIndex
          ).get();
        }
        return new NPNode(
            new Token(tName.start(), value.token().end()),
            tName.trimmedContent(s),
            value
        );
      };
    }
  }

  private record NPSNode(Token token, List<NPNode> children) implements Node {
    static Parser<NPSNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        List<NPNode> nodes = new ArrayList<>();
        try {
          nodes.add(NPNode.parser().parse(s, i, NPSNode.class, i));
        } catch (ParsingException e) {
          //ignore
        }
        while (!nodes.isEmpty()) {
          Optional<Token> ot = TokenType.LIST_SEPARATOR.next(s, nodes.get(nodes.size() - 1).token().end());
          if (ot.isEmpty()) {
            break;
          }
          nodes.add(NPNode.parser().parse(s, ot.get().end(), NPSNode.class, i));
        }
        return new NPSNode(new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()), nodes);
      };
    }
  }

  private record SNode(Token token, String value) implements Node {
    static Parser<SNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> TokenType.STRING.next(s, i).map(t -> new SNode(
          t,
          t.trimmedUnquotedContent(s)
      )).orElseThrow(tokenPE(
          TokenType.STRING,
          i,
          s,
          parentNodeClass,
          parentIndex
      ));
    }
  }

  private record SSNode(Token token, List<SNode> children) implements Node {
    static Parser<SSNode> parser() {
      return (s, i, parentNodeClass, parentIndex) -> {
        List<SNode> nodes = new ArrayList<>();
        try {
          nodes.add(SNode.parser().parse(s, i, SSNode.class, i));
        } catch (ParsingException e) {
          //ignore
        }
        while (!nodes.isEmpty()) {
          Optional<Token> sepT = TokenType.LIST_SEPARATOR.next(s, nodes.get(nodes.size() - 1).token().end());
          if (sepT.isEmpty()) {
            break;
          }
          nodes.add(SNode.parser().parse(s, sepT.get().end(), SSNode.class, i));
        }
        return new SSNode(new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()), nodes);
      };
    }
  }

  private static Supplier<ParsingException> tokenPE(
      TokenType tokenType,
      int i,
      String s,
      Class<? extends Node> parentNode,
      int parentNodeIndex
  ) {
    return () -> new TokenParsingException(List.of(), s, i, parentNode, parentNodeIndex, tokenType);
  }

  private static NamedParamMap from(ENode eNode) {
    return new MapNamedParamMap(
        eNode.name(),
        eNode.child().children().stream()
            .filter(n -> n.value() instanceof DNode)
            .collect(Collectors.toMap(NPNode::name, n -> ((DNode) n.value()).value().doubleValue())),
        eNode.child().children().stream()
            .filter(n -> n.value() instanceof SNode)
            .collect(Collectors.toMap(NPNode::name, n -> ((SNode) n.value()).value())),
        eNode.child().children().stream()
            .filter(n -> n.value() instanceof ENode)
            .collect(Collectors.toMap(NPNode::name, n -> from((ENode) n.value))),
        eNode.child().children().stream()
            .filter(n -> n.value() instanceof LDNode)
            .collect(Collectors.toMap(
                NPNode::name,
                n -> ((LDNode) n.value()).child().children().stream().map(c -> c.value().doubleValue()).toList()
            )),
        eNode.child().children().stream()
            .filter(n -> n.value() instanceof LSNode)
            .collect(Collectors.toMap(
                NPNode::name,
                n -> ((LSNode) n.value()).child().children().stream().map(SNode::value).toList()
            )),
        eNode.child().children().stream()
            .filter(n -> n.value() instanceof LENode)
            .collect(Collectors.toMap(
                NPNode::name,
                n -> ((LENode) n.value()).child().children().stream().map(StringParser::from).toList()
            ))

    );
  }

  public static NamedParamMap parse(String s) {
    return from(ENode.parser().parse(s, 0, null, 0));
  }

  private static <T> List<T> withAppended(List<T> ts, T t) {
    List<T> newTs = new ArrayList<>(ts);
    newTs.add(t);
    return newTs;
  }

}
