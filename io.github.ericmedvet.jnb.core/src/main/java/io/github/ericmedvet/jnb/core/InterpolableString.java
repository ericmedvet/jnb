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

import java.util.function.Function;

public interface InterpolableString {

  String interpolate(ParamMap paramMap);

  static InterpolableString from(String format) {
    return from(pm -> Interpolator.interpolate(format, pm), format);
  }

  static InterpolableString from(Function<ParamMap, String> interpolator, String representation) {
    return new InterpolableString() {
      @Override
      public String interpolate(ParamMap paramMap) {
        return interpolator.apply(paramMap);
      }

      @Override
      public String toString() {
        return representation;
      }
    };
  }

  default InterpolableString and(InterpolableString other) {
    return from(
        pm -> interpolate(pm) + other.interpolate(pm),
        this + other.toString()
    );
  }

  default InterpolableString prefixed(String string) {
    return from(
        pm -> string + interpolate(pm),
        string + this
    );
  }

  default InterpolableString suffixed(String string) {
    return from(
        pm -> interpolate(pm) + string,
        this + string
    );
  }

}
