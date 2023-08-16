package io.github.ericmedvet.jnb.core.parsing;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public class MyParser implements Parser {

  private final static int CONTEXT_SIZE = 10;
  private final String s;
  private final List<Call> calls;
  public MyParser(String s) {
    this.s = s;
    calls = new ArrayList<>();
  }

  private record Call(int i, Class<? extends Node> nodeClass, TokenType tokenType, Token token) {
    public String toPrettyString(String s) {
      int start = Math.max(0, i - CONTEXT_SIZE);
      int end = Math.min(s.length(), i + CONTEXT_SIZE);
      return "%s %sfound @%d in [%d:%s]`%s`".formatted(
          Objects.isNull(nodeClass) ? ("`" + tokenType.rendered() + "`") : nodeClass.getSimpleName(),
          Objects.isNull(token) ? "not " : "",
          i,
          start,
          Objects.isNull(token) ? "..." : token.end(),
          s.substring(Objects.isNull(token) ? start : token.start(), Objects.isNull(token) ? end : token.end())
              .replaceAll("[\\n\\r]", "¶")
              .replaceAll("\\s\\s+", "␣")
      );
    }
  }

  public static void main(String[] args) {
    MyParser parser = new MyParser("person(x=33;names=[eric])");
    parser = new MyParser("person(name = \"Mario Rossi\"; pet = pet(booleans = [true]))");
    System.out.println(parser.parse());
  }

  private static <T> List<T> withAppended(List<T> ts, T t) {
    List<T> newTs = new ArrayList<>(ts);
    newTs.add(t);
    return newTs;
  }

  private IllegalArgumentException buildException() {
    Call lastErrorCall = calls.stream()
        .filter(c -> c.token == null)
        .max(Comparator.comparingInt(c -> c.i))
        .orElseThrow();
    List<Call> lastErrorCalls = calls.stream()
        .filter(c -> c.token == null)
        .filter(c -> c.i == lastErrorCall.i)
        .toList();
    Call lastSuccessfullCall = calls.stream()
        .filter(c -> c.token != null)
        .filter(c -> c.token.end() == lastErrorCall.i())
        .max(Comparator.comparingInt(c -> c.token.length()))
        .orElseThrow();
    return new IllegalArgumentException("Syntax error: %s; last successful match: %s".formatted(
        lastErrorCalls.stream().map(c -> c.toPrettyString(s)).collect(Collectors.joining("; ")),
        lastSuccessfullCall.toPrettyString(s)
    ));
  }

  @Override
  public <N extends Node> Optional<N> parse(int i, Class<N> nodeClass) {
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
    return TokenType.NUM.next(s, i).map(t -> new DNode(
        t,
        Double.parseDouble(t.trimmedContent(s))
    ));
  }

  private Optional<DSNode> parseDS(int i) {
    List<DNode> nodes = new ArrayList<>();
    nodes.add(parse(i, DNode.class).orElseThrow());
    while (true) {
      Optional<Token> sepT = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
      if (sepT.isEmpty()) {
        break;
      }
      nodes.add(parse(sepT.get().end(), DNode.class).orElseThrow());
    }
    return Optional.of(new DSNode(
        new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()),
        nodes
    ));
  }

  private Optional<ENode> parseE(int i) {
    Token tName = parse(i, TokenType.NAME).orElseThrow();
    Token tOpenPar = parse(tName.end(), TokenType.OPEN_CONTENT).orElseThrow();
    NPSNode npsNode = parse(tOpenPar.end(), NPSNode.class).orElseThrow();
    Token tClosedPar = parse(npsNode.token().end(), TokenType.CLOSED_CONTENT).orElseThrow();
    return Optional.of(new ENode(
        new Token(tName.start(), tClosedPar.end()),
        npsNode,
        tName.trimmedContent(s)
    ));
  }

  private Optional<ESNode> parseES(int i) {
    List<ENode> nodes = new ArrayList<>();
    nodes.add(parse(i, ENode.class).orElseThrow());
    while (true) {
      Optional<Token> sepT = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
      if (sepT.isEmpty()) {
        break;
      }
      nodes.add(parse(sepT.get().end(), ENode.class).orElseThrow());
    }
    return Optional.of(new ESNode(new Token(
        i,
        nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()
    ), nodes));
  }

  private Optional<LDNode> parseLD(int i) {
    //case: interval
    try {
      Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
      DNode minDNode = parse(openT.end(), DNode.class).orElseThrow();
      Token sep1 = parse(minDNode.token().end(), TokenType.INTERVAL_SEPARATOR).orElseThrow();
      DNode stepDNode = parse(sep1.end(), DNode.class).orElseThrow();
      Token sep2 = parse(stepDNode.token().end(), TokenType.INTERVAL_SEPARATOR).orElseThrow();
      DNode maxDNode = parse(sep2.end(), DNode.class).orElseThrow();
      Token closedT = parse(maxDNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
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
      return Optional.of(new LDNode(
          new Token(openT.start(), closedT.end()),
          new DSNode(
              new Token(minDNode.token().start(), maxDNode.token().start()),
              dNodes
          )
      ));
    } catch (RuntimeException e) {
      //ignore
    }
    //case: list of values
    try {
      Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
      DSNode dsNode = parse(openT.end(), DSNode.class).orElseThrow();
      Token closedT = parse(dsNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
      return Optional.of(new LDNode(new Token(openT.start(), closedT.end()), dsNode));
    } catch (RuntimeException e) {
      //ignore
    }
    //nothing
    throw new IllegalArgumentException("No valid case");
  }

  private Optional<LENode> parseLE(int i) {
    //case: list with join
    try {
      Token openT = parse(i, TokenType.OPEN_CONTENT).orElseThrow();
      NPNode npNode = parse(openT.end(), NPNode.class).orElseThrow();
      Token closedT = parse(npNode.token().end(), TokenType.CLOSED_CONTENT).orElseThrow();
      Token jointT = parse(closedT.end(), TokenType.LIST_JOIN).orElseThrow();
      LENode outerLENode = parse(jointT.end(), LENode.class).orElseThrow();
      //do cartesian product
      List<ENode> originalENodes = outerLENode.child().children();
      List<ENode> eNodes = new ArrayList<>();
      for (ENode originalENode : originalENodes) {
        if (npNode.value() instanceof DNode || npNode.value() instanceof SNode || npNode.value() instanceof ENode) {
          eNodes.add(new ENode(
              originalENode.token(),
              new NPSNode(
                  originalENode.child().token(),
                  withAppended(originalENode.child().children(), npNode)
              ),
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
      return Optional.of(new LENode(
          new Token(openT.start(), outerLENode.token().end()),
          new ESNode(
              new Token(npNode.token().start(), outerLENode.token().end()),
              eNodes
          )
      ));
    } catch (RuntimeException e) {
      //ignore
    }
    //case: list with mult
    try {
      Token multToken = parse(i, TokenType.I_NUM).orElseThrow();
      int mult = Integer.parseInt(multToken.trimmedContent(s));
      Token jointT = parse(multToken.end(), TokenType.LIST_JOIN).orElseThrow();
      LENode originalLENode = parse(jointT.end(), LENode.class).orElseThrow();
      //multiply
      List<ENode> eNodes = new ArrayList<>();
      for (int j = 0; j < mult; j++) {
        eNodes.addAll(originalLENode.child().children());
      }
      return Optional.of(new LENode(new Token(
          multToken.start(),
          originalLENode.token().end()
      ), new ESNode(
          new Token(originalLENode.token().start(), originalLENode.token().end()),
          eNodes
      )));
    } catch (RuntimeException e) {
      //ignore
    }
    //case: list with concat
    try {
      Token firstConcatT = parse(i, TokenType.LIST_CONCAT).orElseThrow();
      LENode firstLENode = parse(firstConcatT.end(), LENode.class).orElseThrow();
      Token secondConcatT = parse(firstLENode.token().end(), TokenType.LIST_CONCAT).orElseThrow();
      LENode secondLENode = parse(secondConcatT.end(), LENode.class).orElseThrow();
      //concat
      List<ENode> eNodes = new ArrayList<>(firstLENode.child().children());
      eNodes.addAll(secondLENode.child().children());
      return Optional.of(new LENode(new Token(
          firstConcatT.start(),
          secondLENode.token().end()
      ), new ESNode(
          new Token(firstConcatT.start(), secondLENode.token().end()),
          eNodes
      )));
    } catch (RuntimeException e) {
      //ignore
    }
    //case: just list
    try {
      Token openT = parse(i, TokenType.OPEN_LIST).orElseThrow();
      ESNode sNode = parse(openT.end(), ESNode.class).orElseThrow();
      Token closedT = parse(sNode.token().end(), TokenType.CLOSED_LIST).orElseThrow();
      return Optional.of(new LENode(new Token(openT.start(), closedT.end()), sNode));
    } catch (RuntimeException e) {
      //ignore
    }
    //nothing
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
    for (Class<? extends Node> nodeClass : List.of(
        ENode.class,
        LENode.class,
        DNode.class,
        SNode.class,
        LDNode.class,
        LSNode.class
    )) {
      try {
        oNode = parse(tAssign.end(), nodeClass);
        break;
      } catch (RuntimeException e) {
        //ignore
      }
    }
    Node value = oNode.orElseThrow();
    return Optional.of(new NPNode(
        new Token(tName.start(), value.token().end()),
        tName.trimmedContent(s),
        value
    ));
  }

  private Optional<NPSNode> parseNPS(int i) {
    List<NPNode> nodes = new ArrayList<>();
    nodes.add(parse(i, NPNode.class).orElseThrow());
    while (true) {
      Optional<Token> ot = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
      if (ot.isEmpty()) {
        break;
      }
      nodes.add(parse(ot.get().end(), NPNode.class).orElseThrow());
    }
    return Optional.of(new NPSNode(
        new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()),
        nodes
    ));
  }

  private Optional<SNode> parseS(int i) {
    return parse(i, TokenType.STRING).map(t -> new SNode(
        t,
        t.trimmedUnquotedContent(s)
    ));
  }

  private Optional<SSNode> parseSS(int i) {
    List<SNode> nodes = new ArrayList<>();
    nodes.add(parse(i, SNode.class).orElseThrow());
    while (true) {
      Optional<Token> sepT = parse(nodes.get(nodes.size() - 1).token().end(), TokenType.LIST_SEPARATOR);
      if (sepT.isEmpty()) {
        break;
      }
      nodes.add(parse(sepT.get().end(), SNode.class).orElseThrow());
    }
    return Optional.of(new SSNode(
        new Token(i, nodes.isEmpty() ? i : nodes.get(nodes.size() - 1).token().end()),
        nodes
    ));
  }
}
