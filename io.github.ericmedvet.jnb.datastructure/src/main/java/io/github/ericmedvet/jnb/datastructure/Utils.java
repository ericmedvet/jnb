/*-
 * ========================LICENSE_START=================================
 * jnb-datastructure
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
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
package io.github.ericmedvet.jnb.datastructure;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

  private static final Logger L = Logger.getLogger(Utils.class.getName());

  private Utils() {
  }

  public static <K> List<K> concat(List<List<? extends K>> lists) {
    return lists.stream().flatMap(List::stream).collect(Collectors.toList());
  }

  public static void doOrLog(
      Runnable runnable,
      Logger logger,
      Level level,
      Function<Throwable, String> messageFunction
  ) {
    try {
      runnable.run();
    } catch (Throwable t) {
      logger.log(level, messageFunction.apply(t));
    }
  }

  public static String getUserMachineName() {
    String user = System.getProperty("user.name");
    String hostName = "unknown";
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      // ignore
    }
    return user + "@" + hostName;
  }

  public static File robustGetFile(String pathName, boolean overwrite) throws IOException {
    // create directory
    Path path = Path.of(pathName);
    Path filePath = path.getFileName();
    Path dirsPath;
    boolean exist = false;
    if (path.getNameCount() > 1) {
      // create directories
      dirsPath = path.subpath(0, path.getNameCount() - 1);
      Files.createDirectories(dirsPath);
    } else {
      dirsPath = Path.of(".");
    }
    if (!overwrite) {
      // check file existence
      while (dirsPath.resolve(filePath).toFile().exists()) {
        exist = true;
        String newName = null;
        Matcher mNum = Pattern.compile("\\((?<n>[0-9]+)\\)\\.\\w+$").matcher(filePath.toString());
        if (mNum.find()) {
          int n = Integer.parseInt(mNum.group("n"));
          newName = new StringBuilder(filePath.toString())
              .replace(mNum.start("n"), mNum.end("n"), Integer.toString(n + 1))
              .toString();
        }
        Matcher mExtension = Pattern.compile("\\.\\w+$").matcher(filePath.toString());
        if (newName == null && mExtension.find()) {
          newName = new StringBuilder(filePath.toString())
              .replace(mExtension.start(), mExtension.end(), ".(1)" + mExtension.group())
              .toString();
        }
        if (newName == null) {
          newName = filePath + ".newer";
        }
        filePath = Path.of(newName);
      }
      if (exist) {
        L.log(
            Level.WARNING,
            String.format(
                "Given file path '%s' exists; will write on '%s'",
                dirsPath.resolve(path),
                dirsPath.resolve(filePath)
            )
        );
      }
    }
    return dirsPath.resolve(filePath).toFile();
  }
}
