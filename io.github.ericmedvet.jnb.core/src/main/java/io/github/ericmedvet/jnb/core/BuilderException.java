/*-
 * ========================LICENSE_START=================================
 * jnb-core
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
package io.github.ericmedvet.jnb.core;

/// Exception thrown when a builder fails to build an object.
public class BuilderException extends RuntimeException {

  /// Constructs a new builder exception with the specified detail message.
  ///
  /// @param message the detail message
  public BuilderException(String message) {
    super(message);
  }

  /// Constructs a new builder exception with the specified detail message and cause.
  ///
  /// @param message the detail message
  /// @param cause   the cause
  public BuilderException(String message, Throwable cause) {
    super(message, cause);
  }
}
