/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.collection;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This class can capture 2 references of 2 types and set or clear the data using setFirst() / getFirst() and
 * setSecond() / getSecond(). It can be used to return multiple objects of a method, or to easily capture multiple
 * objects without creating their own class.
 *
 * @param <F> the first type, which you want to defined
 * @param <S> the second type which you want to defined
 */
@ToString
@EqualsAndHashCode
public class Pair<F, S> {

  /**
   * The reference of the first value and the type of F
   *
   * @see F
   */
  protected F first;

  /**
   * The reference of the second value and the type of S
   *
   * @see S
   */
  protected S second;

  public Pair(F first, S second) {
    this.first = first;
    this.second = second;
  }

  public Pair() {
  }

  public F getFirst() {
    return this.first;
  }

  public void setFirst(F first) {
    this.first = first;
  }

  public S getSecond() {
    return this.second;
  }

  public void setSecond(S second) {
    this.second = second;
  }

}
