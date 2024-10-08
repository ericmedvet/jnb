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

/** @author "Eric Medvet" on 2023/10/18 for jnb */
public record ProjectInfo(String name, Version version, String buildDate) {
  public record Version(String major, String minor, String patch) {
    @Override
    public String toString() {
      return major + "." + minor + "." + patch;
    }
  }

  @Override
  public String toString() {
    return name + ":" + version + ":" + buildDate;
  }

  public String toShortString() {
    return name + ":" + version;
  }
}
