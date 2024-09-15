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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/** @author "Eric Medvet" on 2023/10/18 for jnb */
public class ProjectInfoProvider {

  private static final Map<Class<?>, Optional<ProjectInfo>> CACHE = new HashMap<>();

  private ProjectInfoProvider() {}

  public static Optional<ProjectInfo> of(Class<?> clazz) {
    return CACHE.computeIfAbsent(clazz, c -> {
      Properties properties = new Properties();
      try (InputStream resource = c.getResourceAsStream("/project-info.props")) {
        if (resource == null) {
          return Optional.empty();
        }
        properties.load(resource);
        String majorVersion = "";
        String minorVersion = "";
        String patchVersion = "";
        if (properties.getProperty("version") != null) {
          String[] versionTokens = properties.getProperty("version").split("\\.");
          majorVersion = versionTokens[0];
          if (versionTokens.length > 1) {
            minorVersion = versionTokens[1];
          }
          if (versionTokens.length > 2) {
            patchVersion = versionTokens[2];
          }
        }
        return Optional.of(new ProjectInfo(
            properties.getProperty("name") == null ? "" : properties.getProperty("name"),
            new ProjectInfo.Version(majorVersion, minorVersion, patchVersion),
            properties.getProperty("build.timestamp") == null
                ? ""
                : properties.getProperty("build.timestamp")));
      } catch (IOException e) {
        // info props not found: ignore
        return Optional.empty();
      }
    });
  }
}
