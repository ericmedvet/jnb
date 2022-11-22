/*
 * Copyright 2022 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.units.malelab.jnb;

import it.units.malelab.jnb.core.Param;

import java.util.List;

public class Starter {

  record Office(@Param("roomNumber") int roomNumber, @Param("head") Person head, @Param("staff") List<Person> staff) {}

  record Person(@Param("firstName") String firstName, @Param("lastName") String lastName, @Param("age") int age) {}

  public static void main(String[] args) {
    System.out.println("Hello world!");
  }
}
