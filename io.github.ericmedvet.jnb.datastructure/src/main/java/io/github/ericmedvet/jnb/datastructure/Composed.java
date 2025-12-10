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

import java.util.Optional;

/// An object which internally has another object of type `C`. Typically used to model the fact that
/// an instance of a given type is based on another type `C`.
///
/// @param <C> the type of the inner object
public interface Composed<C> {

  /// Recursively searches for the deepest object of a specific class within a composed object
  /// structure. If the object is a `Composed` object, the search continues recursively into its
  /// inner object. Otherwise, if the object is of the provided class, returns it.
  ///
  /// @param object the object to search within
  /// @param kClass the class of the object to find
  /// @param <K>    the type of the object to find
  /// @return an `Optional` containing the deepest object of type `K` if found, otherwise an empty
  /// `Optional`
  static <K> Optional<K> deepest(Object object, Class<K> kClass) {
    // If the current object is composed, delegate to its deepest method
    if (object instanceof Composed<?> c) {
      return c.deepest(kClass);
    }
    if (kClass.isAssignableFrom(object.getClass())) {
      //noinspection unchecked
      return Optional.of((K) object);
    }
    return Optional.empty();
  }

  /// Recursively searches for the shallowest object of a specific class within a composed object
  /// structure. If the object itself is of the specified class, it is returned. Otherwise, if the
  /// object is a `Composed` object, the search continues recursively into its inner object. The
  /// "shallowest" means the first one encountered from the outside in.
  ///
  /// @param object the object to search within
  /// @param kClass the class of the object to find
  /// @param <K>    the type of the object to find
  /// @return an `Optional` containing the shallowest object of type `K` if found, otherwise an
  /// empty `Optional`
  static <K> Optional<K> shallowest(Object object, Class<K> kClass) {
    if (kClass.isAssignableFrom(object.getClass())) {
      //noinspection unchecked
      return Optional.of((K) object);
    }
    if (object instanceof Composed<?> c) {
      return c.shallowest(kClass);
    }
    return Optional.empty();
  }

  /// Returns the deepest non-`Composed` object within this composed object structure. Works as
  /// [#deepest(Class)] called with `Object` as class.
  ///
  /// @return the deepest object
  default Object deepest() {
    if (inner() instanceof Composed<?> composed) {
      return composed.deepest();
    }
    return inner();
  }

  /// Recursively searches for the deepest object of a specific class within this composed object
  /// structure. If the inner object is a `Composed` object, the search continues recursively into
  /// its inner object. Otherwise, if it is of the provided class, returns it.
  ///
  /// @param kClass the class of the object to find
  /// @param <K>    the type of the object to find
  /// @return an `Optional` containing the deepest object of type `K` if found, otherwise an empty
  /// `Optional`
  default <K> Optional<K> deepest(Class<K> kClass) {
    if (inner() instanceof Composed<?> composed) {
      Optional<K> inside = composed.deepest(kClass);
      if (inside.isPresent()) {
        return inside;
      }
    }
    if (kClass.isAssignableFrom(inner().getClass())) {
      //noinspection unchecked
      return Optional.of((K) inner());
    }
    if (kClass.isAssignableFrom(getClass())) {
      //noinspection unchecked
      return Optional.of((K) this);
    }
    return Optional.empty();
  }

  /// Returns the inner object of this composed object.
  ///
  /// @return the inner object
  C inner();

  /// Recursively searches for the shallowest object of a specific class within this composed object
  /// structure. If this object itself is of the specified class, it is returned. Otherwise, the
  /// search continues recursively into this inner object. The "shallowest" means the first one
  /// encountered from the outside in.
  ///
  /// @param kClass the class of the object to find
  /// @param <K>    the type of the object to find
  /// @return an `Optional` containing the shallowest object of type `K` if found, otherwise an
  /// empty `Optional`
  default <K> Optional<K> shallowest(Class<K> kClass) {
    if (kClass.isAssignableFrom(getClass())) {
      //noinspection unchecked
      return Optional.of((K) this);
    }
    if (kClass.isAssignableFrom(inner().getClass())) {
      //noinspection unchecked
      return Optional.of((K) inner());
    }
    if (inner() instanceof Composed<?> composed) {
      Optional<K> inside = composed.shallowest(kClass);
      if (inside.isPresent()) {
        return inside;
      }
    }
    return Optional.empty();
  }
}
