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
