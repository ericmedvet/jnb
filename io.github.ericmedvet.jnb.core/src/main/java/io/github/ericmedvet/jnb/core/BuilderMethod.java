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

import java.lang.annotation.*;

/// Indicates that the target method or constructor is to be consumed by
/// [NamedBuilder#fromDiscovery(String...)] (and related methods) to build a [DocumentedBuilder].
/// This annotation is only *necessary*:
/// - when using [NamedBuilder#fromClass(Class)] on a class with more than one constructor or
/// - when one wants to change the name of the built builder (see
/// [AutoBuiltDocumentedBuilder#from(java.lang.reflect.Executable, Alias\[\])]).
///
/// Otherwise, it is optional.
///
/// @see NamedBuilder#fromClass(Class)
/// @see NamedBuilder#fromUtilityClass(Class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface BuilderMethod {

  /// The name of the builder built from the annotated method or constructor.
  ///
  /// @return the name of the builder built from the annotated method or constructor
  String value() default "";
}
