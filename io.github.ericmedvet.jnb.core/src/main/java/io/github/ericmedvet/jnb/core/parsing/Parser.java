package io.github.ericmedvet.jnb.core.parsing;

import io.github.ericmedvet.jnb.core.MapNamedParamMap;
import io.github.ericmedvet.jnb.core.NamedParamMap;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author "Eric Medvet" on 2023/08/16 for jnb
 */
public interface Parser {

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

  <N extends Node> Optional<N> parse(int i, Class<N> nodeClass);

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
            .collect(Collectors.toMap(NPNode::name, n -> from((ENode) n.value()))),
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
                n -> ((LENode) n.value()).child().children().stream().map(Parser::from).toList()
            ))

    );
  }

  default NamedParamMap parse() {
    return from(parse(0, ENode.class).orElseThrow());
  }
}
