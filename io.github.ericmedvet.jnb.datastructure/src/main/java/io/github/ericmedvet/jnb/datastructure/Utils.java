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

import io.github.ericmedvet.jnb.core.MapNamedParamMap;
import io.github.ericmedvet.jnb.core.NamedParamMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

public class Utils {

  private static final Logger L = Logger.getLogger(Utils.class.getName());

  private Utils() {
  }

  public static <T, R> Function<T, R> cached(Function<T, R> f) {
    Map<T, R> cache = new WeakHashMap<>();
    return t -> cache.computeIfAbsent(t, f);
  }

  public static <K> List<K> concat(List<List<? extends K>> lists) {
    return lists.stream().flatMap(List::stream).collect(Collectors.toList());
  }

  @SafeVarargs
  public static <K> List<K> concat(List<K>... lists) {
    return Arrays.stream(lists).flatMap(List::stream).collect(Collectors.toList());
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

  public static <E> List<E> fold(List<E> items, int fold, int n) {
    return folds(items, List.of(fold), n);
  }

  public static <E> List<E> folds(List<E> items, List<Integer> folds, int n) {
    return IntStream.range(0, items.size())
        .filter(i -> folds.contains(i % n))
        .mapToObj(items::get)
        .toList();
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

  public static <E> List<E> negatedFold(List<E> items, int fold, int n) {
    return folds(items, IntStream.range(0, n).filter(j -> j != fold).boxed().toList(), n);
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

  public static void save(Object o, String filePath, boolean overwrite, boolean verbose) {
    File file = null;
    try {
      file = io.github.ericmedvet.jnb.datastructure.Utils.robustGetFile(filePath, overwrite);
      switch (o) {
        case BufferedImage image -> ImageIO.write(image, "png", file);
        case String s -> Files.writeString(
            file.toPath(),
            s,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        case Binarizable binarizable -> Files.write(
            file.toPath(),
            binarizable.data(),
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        case byte[] data -> {
          try (OutputStream os = new FileOutputStream(file)) {
            os.write(data);
          }
        }
        case NamedParamMap npm -> Files.writeString(
            file.toPath(),
            MapNamedParamMap.prettyToString(npm),
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        case null -> throw new IllegalArgumentException("Cannot save null data of type %s");
        default -> throw new IllegalArgumentException(
            "Cannot save data of type %s".formatted(o.getClass().getSimpleName())
        );
      }
      if (verbose) {
        L.info("Saved file at '%s'".formatted(file.getPath()));
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot save '%s'".formatted(Objects.isNull(file) ? filePath : file.getPath()),
          e
      );
    }
  }

  public static <T, K, U> Collector<T, ?, SequencedMap<K, U>> toSequencedMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper
  ) {
    return Collectors.toMap(
        keyMapper,
        valueMapper,
        (u1, u2) -> u1,
        LinkedHashMap::new
    );
  }

  public static <T, U> Collector<T, ?, SequencedMap<T, U>> toSequencedMap(
      Function<? super T, ? extends U> valueMapper
  ) {
    return toSequencedMap(Function.identity(), valueMapper);
  }

}