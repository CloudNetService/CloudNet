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
 * An element that holds a primitive type as it's inner value (one of byte, short, int, long, float, double, boolean,
 * char). In this specific case strings are counted as primitive too.
 *
 * @param key        the key of this element or {@link Element#NO_KEY} if an array entry.
 * @param innerValue the inner primitive value of this element (type must be one of the list given above).
 * @since 4.0
 */
public record PrimitiveElement(@NonNull String key, @NonNull Object innerValue) implements Element {

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull ElementVisitor visitor) {
    visitor.visitPrimitive(this);
  }
}
