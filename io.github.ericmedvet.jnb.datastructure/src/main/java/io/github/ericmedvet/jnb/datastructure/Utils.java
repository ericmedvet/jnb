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

/// This class provides various `static` utility methods.
public class Utils {

  private static final Logger L = Logger.getLogger(Utils.class.getName());

  private Utils() {
  }

  /// Returns a function that caches the results of the given `function`. Internally uses a
  /// `WeakHashMap`.
  ///
  /// @param function the function to be cached
  /// @param <T>      the type of the input to the function
  /// @param <R>      the type of the result of the function
  /// @return a new function that returns cached results if available, otherwise computes and caches
  /// them
  public static <T, R> Function<T, R> cached(Function<T, R> function) {
    Map<T, R> cache = new WeakHashMap<>();
    return t -> cache.computeIfAbsent(t, function);
  }

  /// Concatenates the items of the provided `lists`.
  ///
  /// @param lists a list of lists of items to concatenate
  /// @param <K>   the type of the items in the lists
  /// @return an unmodified lists containing of the items of the provided lists
  public static <K> List<K> concat(List<List<? extends K>> lists) {
    return lists.stream().flatMap(List::stream).collect(Collectors.toList());
  }

  /// Concatenates the items of the provided `lists`.
  ///
  /// @param lists a list of lists of items to concatenate
  /// @param <K>   the type of the items in the lists
  /// @return an unmodified lists containing of the items of the provided lists
  @SafeVarargs
  public static <K> List<K> concat(List<K>... lists) {
    return Arrays.stream(lists).flatMap(List::stream).collect(Collectors.toList());
  }

  /// Attempts to execute the provided `runnable` and log if the execution fails.
  ///
  /// @param runnable        the task to be executed
  /// @param logger          the logger on which to log if the execution of the task fails
  /// @param level           the level of the log message
  /// @param messageFunction a function to generate a log message given the `Throwable` raised by
  ///                        the failed execution of the task
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

  /// Returns a list containing the items of the `fold`-indexed fold of the `n`-folding of the
  /// provided `list`. Internally calls [#folds(List, List, int)].
  ///
  /// @param list the list to fold
  /// @param fold the fold index to include
  /// @param n    the total number of folds
  /// @param <E>  the type of elements in the list
  /// @return a new unmodifiable list containing only the items of the `fold`-th fold
  public static <E> List<E> fold(List<E> list, int fold, int n) {
    return folds(list, List.of(fold), n);
  }

  /// Returns a list containing the items in the `folds`-indexed folds of the `n`-folding of the
  /// provided `list`. An `n`-folding is a list of approximately equal-sized disjoint sublists of
  /// the list that, if concatenated, form the list itself. The folds are built using the modulo
  /// operator: i.e., the third fold contains the items of the list whose index modulo `n` is 2.
  ///
  /// @param list  the list to fold
  /// @param folds a list of fold indices to include
  /// @param n     the total number of folds
  /// @param <E>   the type of elements in the list
  /// @return a new unmodifiable list containing only the items of the `folds`-indexed folds
  public static <E> List<E> folds(List<E> list, List<Integer> folds, int n) {
    return IntStream.range(0, list.size())
        .filter(i -> folds.contains(i % n))
        .mapToObj(list::get)
        .toList();
  }

  /// Returns a string representing the current user and machine name. The format is
  /// "username@hostname". The method obtains the user through the `"user.name"` system property
  /// (accessed through [System#getProperty(String)]) and the machine names through
  /// [InetAddress#getHostName()] invoked on the local host address.
  ///
  /// @return a string identifying the user and machine
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

  /// Returns a list containing the items not belonging to the `fold`-indexed fold of the
  /// `n`-folding of the provided `list`. Internally calls [#folds(List, List, int)].
  ///
  /// @param list the list to fold
  /// @param fold the fold index to include
  /// @param n    the total number of folds
  /// @param <E>  the type of elements in the list
  /// @return a new unmodifiable list containing all but the items of the `fold`-th fold
  public static <E> List<E> negatedFold(List<E> list, int fold, int n) {
    return folds(list, IntStream.range(0, n).filter(j -> j != fold).boxed().toList(), n);
  }

  /// Returns the `File` object representing the given `pathName`. If `overwrite` is `false` and a
  /// file with the same name already exists, it generates a new unique file name by appending a
  /// number or ".newer" suffix. The method creates the directories leading to the file if they do
  /// not exist.
  ///
  /// @param pathName  the desired path for the file
  /// @param overwrite if `false`, the returned file represents a renamed version of the given path;
  ///                  otherwise, represents the given path
  /// @return a `File` object representing the (potentially new) file path
  /// @throws IOException if an I/O error occurs during directory creation or file path resolution
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

  /// Saves an object to a file. The type of saving depends on the object type. A `BufferedImage` is
  /// saved as a PNG image through [ImageIO#write(RenderedImage, String, File)]. A `String` is saved
  /// as text. A `Binarizable` object is saved as raw binary data (obtained through
  /// [Binarizable#data()]). A `byte[]` is saved as raw binary data. A `NamedParamMap` is saved as a
  /// text, pretty-printed through [MapNamedParamMap#prettyToString()].
  ///
  /// @param object    the object to save
  /// @param filePath  the path to the file where the object will be saved
  /// @param overwrite if `true`, overwrites the file at `filePath`, if any; otherwise, obtain a new
  ///                  path through [#robustGetFile(String, boolean)]
  /// @param verbose   if `true`, prints a log message upon successful saving
  /// @throws RuntimeException if an `IOException` occurs during saving, or if the object type is
  ///                          not supported
  public static void save(Object object, String filePath, boolean overwrite, boolean verbose) {
    File file = null;
    try {
      file = io.github.ericmedvet.jnb.datastructure.Utils.robustGetFile(filePath, overwrite);
      switch (object) {
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
            "Cannot save data of type %s".formatted(object.getClass().getSimpleName())
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

  /// Returns a `Collector` that accumulates elements into a `SequencedMap`. The keys and values are
  /// produced by applying the provided mapping functions to the input elements. If multiple input
  /// elements map to the same key, the value from the first encountered element is retained. The
  /// order of elements in the input stream is preserved in the resulting map.
  ///
  /// @param keyMapper   a function to extract the key from an input element
  /// @param valueMapper a function to extract the value from an input element
  /// @param <T>         the type of the input elements
  /// @param <K>         the type of the keys in the map
  /// @param <U>         the type of the values in the map
  /// @return a `Collector` that accumulates elements into a `SequencedMap`
  public static <T, K, U> Collector<T, ?, SequencedMap<K, U>> toSequencedMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper
  ) {
    return Collectors.toMap(
        keyMapper,
        valueMapper,
        (u1, _) -> u1,
        LinkedHashMap::new
    );
  }

  /// Returns a `Collector` that accumulates elements into a `SequencedMap`, where the input elements
  /// themselves are the keys. The values are produced by applying the provided mapping function to
  /// the input elements. Internally calls [#toSequencedMap(Function, Function)] with the identity as the first argument.
  ///
  /// @param valueMapper a function to extract the value from an input element
  /// @param <T>         the type of the input elements (and keys in the map)
  /// @param <U>         the type of the values in the map
  /// @return a `Collector` that accumulates elements into a `SequencedMap`
  public static <T, U> Collector<T, ?, SequencedMap<T, U>> toSequencedMap(
      Function<? super T, ? extends U> valueMapper
  ) {
    return toSequencedMap(Function.identity(), valueMapper);
  }

}