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
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents an object element. An object element contains key-value mappings (can be imagined like a tree).
 *
 * @param key      the key of this element or {@link Element#NO_KEY} if an array entry or the root object.
 * @param elements the elements of this object element, each given element must have a key present.
 * @since 4.0
 */
public record ObjectElement(
  @NonNull String key,
  @NonNull @Unmodifiable Collection<? extends Element> elements
) implements Element {

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull ElementVisitor visitor) {
    var objectSectionVisitor = visitor.visitObject(this);
    this.elements.forEach(element -> element.accept(objectSectionVisitor));
    objectSectionVisitor.visitEnd();
  }
}
