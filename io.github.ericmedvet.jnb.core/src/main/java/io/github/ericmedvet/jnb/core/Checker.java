package io.github.ericmedvet.jnb.core;

import io.github.ericmedvet.jnb.core.parsing.StringParser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

public class Checker {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("A single arg with the path of the file to be tested is expected");
      System.exit(0);
    }
    try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
      String s = br.lines().collect(Collectors.joining("\n"));
      NamedParamMap npm = StringParser.parse(s);
      System.out.println(MapNamedParamMap.prettyToString(npm));
    }
  }
}
