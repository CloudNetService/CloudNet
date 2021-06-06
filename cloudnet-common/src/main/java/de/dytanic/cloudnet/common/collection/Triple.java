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
 * This class can capture 3 references of 3 types and set or clear the data using setFirst() / getFirst() and
 * setSecond() / getSecond(). It can be used to return multiple objects of a method, or to easily capture multiple
 * objects without creating their own class.
 *
 * @param <F> the first type, which you want to defined
 * @param <S> the second type which you want to defined
 * @param <T> the third type which you want to defined
 */
@ToString
@EqualsAndHashCode
public class Triple<F, S, T> {

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

  /**
   * The reference of the third value and the type of T
   *
   * @see T
   */
  protected T third;

  public Triple(F first, S second, T third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public Triple() {
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

  public T getThird() {
    return this.third;
  }

  public void setThird(T third) {
    this.third = third;
  }

}
