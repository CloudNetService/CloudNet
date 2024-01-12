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
 * An element that contains multiple sub elements (of any type). Note that array entry elements always have no key given
 * (see {@link Element#NO_KEY}).
 *
 * @param key     the key of this element or {@link Element#NO_KEY} if an array entry.
 * @param entries the entries of the array element.
 * @since 4.0
 */
public record ArrayElement(
  @NonNull String key,
  @NonNull @Unmodifiable Collection<? extends Element> entries
) implements Element {

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull ElementVisitor visitor) {
    var arraySectionVisitor = visitor.visitArray(this);
    this.entries.forEach(element -> element.accept(arraySectionVisitor));
    arraySectionVisitor.visitEnd();
  }
}
