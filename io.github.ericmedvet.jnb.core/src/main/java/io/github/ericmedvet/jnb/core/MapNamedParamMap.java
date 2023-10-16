package io.github.ericmedvet.jnb.core;

import io.github.ericmedvet.jnb.core.parsing.TokenType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
public class MapNamedParamMap implements NamedParamMap, Formattable {

  public record TypedKey(String name, Type type) implements Comparable<TypedKey> {
    @Override
    public int compareTo(TypedKey o) {
      return name.compareTo(o.name);
    }
  }

  private final String name;
  private final SortedMap<TypedKey, Object> values;

  public SortedMap<TypedKey, Object> getValues() {
    return values;
  }

  public MapNamedParamMap(String name, Map<TypedKey, Object> values) {
    this.name = name;
    this.values = new TreeMap<>();
    for (Map.Entry<TypedKey, Object> e : values.entrySet()) {
      if (e.getKey().type.equals(Type.INT)) {
        this.values.put(
            new TypedKey(e.getKey().name, Type.DOUBLE), Double.valueOf(intValue(e.getValue())));
      } else if (e.getKey().type.equals(Type.BOOLEAN)) {
        this.values.put(
            new TypedKey(e.getKey().name, Type.STRING),
            booleanValue(e.getValue().toString()).toString());
      } else if (e.getKey().type.equals(Type.ENUM)) {
        this.values.put(
            new TypedKey(e.getKey().name, Type.STRING),
            ((Enum<?>) e.getValue()).name().toLowerCase());
      } else if (e.getKey().type.equals(Type.INTS)) {
        this.values.put(
            new TypedKey(e.getKey().name, Type.DOUBLES),
            checkList((List<?>) e.getValue(), MapNamedParamMap::intValue));
      } else if (e.getKey().type.equals(Type.BOOLEANS)) {
        this.values.put(
            new TypedKey(e.getKey().name, Type.STRINGS),
            checkList((List<?>) e.getValue(), b -> booleanValue(b.toString()).toString()));
      } else if (e.getKey().type.equals(Type.ENUMS)) {
        this.values.put(
            new TypedKey(e.getKey().name, Type.STRINGS),
            checkList((List<?>) e.getValue(), v -> ((Enum<?>) v).name().toLowerCase()));
      } else {
        this.values.put(e.getKey(), e.getValue());
      }
    }
  }

  @Override
  public <E extends Enum<E>> Object value(String n, Type type, Class<E> enumClass) {
    return switch (type) {
      case INT -> intValue(values.get(new TypedKey(n, Type.DOUBLE)));
      case BOOLEAN -> booleanValue(values.get(new TypedKey(n, Type.STRING)));
      case ENUM -> enumValue(values.get(new TypedKey(n, Type.STRING)), enumClass);
      case INTS -> checkList(
          (List<?>) values.get(new TypedKey(n, Type.DOUBLES)), MapNamedParamMap::intValue);
      case BOOLEANS -> checkList(
          (List<?>) values.get(new TypedKey(n, Type.STRINGS)), MapNamedParamMap::booleanValue);
      case ENUMS -> checkList(
          (List<?>) values.get(new TypedKey(n, Type.STRINGS)), s -> enumValue(s, enumClass));
      default -> values.get(new TypedKey(n, type));
    };
  }

  private static List<?> checkList(List<?> l, Function<?, ?> mapper) {
    if (l == null) {
      return null;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    List<?> mappedL = l.stream().map(i -> ((Function) mapper).apply(i)).toList();
    if (mappedL.stream().anyMatch(Objects::isNull)) {
      return null;
    }
    return mappedL;
  }

  private static Integer intValue(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number d) {
      return d.intValue() != d.doubleValue() ? null : d.intValue();
    }
    return null;
  }

  private static Boolean booleanValue(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof String s) {
      if (!s.equals("true") && !s.equals("false")) {
        return null;
      }
      return Boolean.valueOf(s);
    }
    return null;
  }

  private static <E extends Enum<E>> E enumValue(Object o, Class<E> enumClass) {
    if (o == null) {
      return null;
    }
    if (o instanceof String s) {
      return Enum.valueOf(enumClass, s.toUpperCase());
    }
    return null;
  }

  private static int currentLineLength(String s) {
    String[] lines = s.split("\n");
    return lines[lines.length - 1].length();
  }

  private static String indent(int w) {
    return IntStream.range(0, w).mapToObj(i -> " ").collect(Collectors.joining());
  }

  private static boolean isInt(Double v) {
    return v.intValue() == v;
  }

  private static String listContentToInlineString(List<?> l, String space) {
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < l.size(); j++) {
      if (l.get(j) instanceof ParamMap m) {
        if (m instanceof NamedParamMap namedParamMap) {
          sb.append(namedParamMap.getName()).append(TokenType.OPEN_CONTENT.rendered());
        }
        sb.append(mapContentToInlineString(m, space));
        if (m instanceof NamedParamMap) {
          sb.append(TokenType.CLOSED_CONTENT.rendered());
        }
      } else if (l.get(j) instanceof String s) {
        sb.append(stringValue(s));
      } else if (l.get(j) instanceof Enum<?> e) {
        sb.append(e.name().toLowerCase());
      } else {
        sb.append(l.get(j).toString());
      }
      if (j < l.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered()).append(space);
      }
    }
    return sb.toString();
  }

  private static void listContentToMultilineString(
      StringBuilder sb, int maxW, int w, int indent, String space, List<?> l) {
    for (int j = 0; j < l.size(); j++) {
      sb.append("\n").append(indent(w + indent + indent));
      if (l.get(j) instanceof NamedParamMap m) {
        prettyToString(m, sb, maxW, w + indent + indent, indent, space);
      } else if (l.get(j) instanceof String s) {
        sb.append(stringValue(s));
      } else if (l.get(j) instanceof Enum<?> e) {
        sb.append(e.name().toLowerCase());
      } else {
        sb.append(l.get(j).toString());
      }
      if (j < l.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered());
      }
    }
    sb.append("\n").append(indent(w + indent));
  }

  private static String mapContentToInlineString(ParamMap m, String space) {
    StringBuilder sb = new StringBuilder();
    List<String> names = new ArrayList<>(m.names());
    for (int i = 0; i < names.size(); i++) {
      sb.append(names.get(i))
          .append(space)
          .append(TokenType.ASSIGN_SEPARATOR.rendered())
          .append(space);
      Object value = m.value(names.get(i));
      if (value instanceof List<?> l) {
        sb.append(TokenType.OPEN_LIST.rendered())
            .append(listContentToInlineString(l, space))
            .append(TokenType.CLOSED_LIST.rendered());
      } else if (value instanceof ParamMap innerMap) {
        if (innerMap instanceof NamedParamMap namedParamMap) {
          sb.append(namedParamMap.getName()).append(TokenType.OPEN_CONTENT.rendered());
        }
        sb.append(mapContentToInlineString(innerMap, space));
        if (innerMap instanceof NamedParamMap) {
          sb.append(TokenType.CLOSED_CONTENT.rendered());
        }
      } else if (value instanceof String) {
        sb.append(stringValue((String) value));
      } else {
        sb.append(value.toString());
      }
      if (i < names.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered()).append(space);
      }
    }
    return sb.toString();
  }

  private static void mapContentToMultilineString(
      StringBuilder sb, int maxW, int w, int indent, String space, ParamMap map) {
    List<String> names = new ArrayList<>(map.names());
    for (int i = 0; i < names.size(); i++) {
      sb.append("\n")
          .append(indent(w + indent))
          .append(names.get(i))
          .append(space)
          .append(TokenType.ASSIGN_SEPARATOR.rendered())
          .append(space);
      Object value = map.value(names.get(i));
      if (value instanceof List<?> l) {
        sb.append(TokenType.OPEN_LIST.rendered());
        String listContent = listContentToInlineString(l, space);
        if (l.isEmpty() || listContent.length() + currentLineLength(sb.toString()) < maxW) {
          sb.append(listContent);
        } else {
          listContentToMultilineString(sb, maxW, w, indent, space, l);
        }
        sb.append(TokenType.CLOSED_LIST.rendered());
      } else if (value instanceof NamedParamMap m) {
        prettyToString(m, sb, maxW, w + indent, indent, space);
      } else if (value instanceof String) {
        sb.append(stringValue((String) value));
      } else {
        sb.append(value.toString());
      }
      if (i < names.size() - 1) {
        sb.append(TokenType.LIST_SEPARATOR.rendered());
      }
    }
    sb.append("\n").append(indent(w));
  }

  @SuppressWarnings("unused")
  public static String prettyToString(ParamMap map) {
    return prettyToString(map, 80);
  }

  public static String prettyToString(ParamMap map, int maxW) {
    StringBuilder sb = new StringBuilder();
    prettyToString(map, sb, maxW, 0, 2, " ");
    return sb.toString();
  }

  public static void prettyToString(
      ParamMap map, StringBuilder sb, int maxW, int w, int indent, String space) {
    // iterate
    if (map instanceof NamedParamMap namedParamMap) {
      sb.append(namedParamMap.getName());
    }
    sb.append(TokenType.OPEN_CONTENT.rendered());
    String content = mapContentToInlineString(map, space);
    if (map.names().isEmpty() || content.length() + currentLineLength(sb.toString()) < maxW) {
      sb.append(content);
    } else {
      mapContentToMultilineString(sb, maxW, w, indent, space, map);
    }
    sb.append(TokenType.CLOSED_CONTENT.rendered());
  }

  private static String stringValue(String value) {
    return value.matches("[A-Za-z][A-Za-z0-9_]*") ? value : ('"' + value + '"');
  }

  @Override
  public Set<String> names() {
    return values.keySet().stream().map(k -> k.name).collect(Collectors.toSet());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return prettyToString(this, Integer.MAX_VALUE);
  }

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    boolean alternate = (flags & FormattableFlags.ALTERNATE) == FormattableFlags.ALTERNATE;
    if (alternate) {
      formatter.format("%s", name);
    } else {
      formatter.format("%s", prettyToString(this, Integer.MAX_VALUE));
    }
  }
}
