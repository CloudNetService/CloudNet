/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.document.send;

import eu.cloudnetservice.driver.document.send.element.ArrayElement;
import eu.cloudnetservice.driver.document.send.element.NullElement;
import eu.cloudnetservice.driver.document.send.element.ObjectElement;
import eu.cloudnetservice.driver.document.send.element.PrimitiveElement;
import lombok.NonNull;

/**
 * A visitor for elements. The visitor can for example be responsible to import all the visited elements into an
 * underlying implementation of a document type.
 *
 * @since 4.0
 */
public interface ElementVisitor {

  /**
   * Called when the end of visit for this visitor is reached. That can for example be when all elements of an array
   * element were processed.
   */
  void visitEnd();

  /**
   * Visits a null element.
   *
   * @param entry the null element to visit.
   * @throws NullPointerException if the given entry is null.
   */
  void visitNull(@NonNull NullElement entry);

  /**
   * Visits a primitive element.
   *
   * @param entry the primitive element to visit.
   * @throws NullPointerException if the given entry is null.
   */
  void visitPrimitive(@NonNull PrimitiveElement entry);

  /**
   * Starts the visit of an array element. The returned element visitor is used to visit each entry of the array.
   *
   * @param entry the array element to visit.
   * @return the visitor to apply to all array entries.
   * @throws NullPointerException if the given entry is null.
   */
  @NonNull ElementVisitor visitArray(@NonNull ArrayElement entry);

  /**
   * Starts the visit of an object element. The returned element visitor is used to visit each key-value mapping of the
   * object.
   *
   * @param entry the object element to visit.
   * @return the visitor to apply to all object key-value mappings.
   * @throws NullPointerException if the given entry is null.
   */
  @NonNull ElementVisitor visitObject(@NonNull ObjectElement entry);
}
