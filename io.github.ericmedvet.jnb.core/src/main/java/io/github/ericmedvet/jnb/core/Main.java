/*-
 * ========================LICENSE_START=================================
 * jnb-core
 * %%
 * Copyright (C) 2023 Eric Medvet
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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.util.List;

/**
 * @author "Eric Medvet" on 2023/10/18 for jnb
 */
@Discoverable
public class Main {

  @Discoverable
  public record Pet(
      @Param("name") String name,
      @Param(value = "kind", dS = "dog") String kind,
      @Param(
              value = "legs",
              dIs = {4})
          List<Integer> legs,
      @Param(
              value = "booleans",
              dBs = {false, false})
          List<Boolean> booleans) {}

  public static void main(String[] args) throws IOException {
    NamedBuilder<Object> nb = NamedBuilder.fromDiscovery("io.github.ericmedvet");
    System.out.println(nb);
  }

  public static Pet pet(String name) {
    return new Pet(name, "dog", List.of(1), List.of());
  }

  private void go() throws IOException {
    System.out.println(ProjectInfoProvider.of(this.getClass()).orElseThrow());
    ScanResult scanResult = new ClassGraph().enableAllInfo().scan();
    for (ClassInfo classInfo :
        scanResult
            .getAllClasses()
            .filter(
                classInfo ->
                    classInfo.hasAnnotation(Discoverable.class)
                        || classInfo.hasDeclaredMethodAnnotation(Discoverable.class))) {
      System.out.println(classInfo);
    }
  }
}
