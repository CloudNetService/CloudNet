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

package eu.cloudnetservice.driver.document.send.element;

import eu.cloudnetservice.driver.document.send.ElementVisitor;
import lombok.NonNull;

/**
 * An abstract element that can be included into any document type. Not all implemented types must be supported by a
 * document type. The default element types are:
 * <ol>
 *   <li>Objects
 *   <li>Arrays
 *   <li>Primitives
 *   <li>Null
 * </ol>
 *
 * @since 4.0
 */
public sealed interface Element permits ArrayElement, NullElement, ObjectElement, PrimitiveElement {

  /**
   * Indicates that an element has no key. This key is only used for array entries at the moment.
   */
  String NO_KEY = "";

  /**
   * Get the key of this element or an empty string ({@link #NO_KEY}) if the element has no key.
   *
   * @return the key of this element.
   */
  @NonNull String key();

  /**
   * Applies the given visitor to this element. It is up to the implementation (and therefore the element type) to
   * decide what steps should be executed on the given element visitor.
   *
   * @param visitor the visitor to apply to this element.
   * @throws NullPointerException if the given visitor is null.
   */
  void accept(@NonNull ElementVisitor visitor);
}
