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

package eu.cloudnetservice.driver.impl.document.empty;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.document.send.element.ObjectElement;
import java.util.Set;
import lombok.NonNull;

/**
 * A document send implementation that contains nothing.
 *
 * @since 4.0
 */
final class EmptyDocumentSend implements DocumentSend {

  /**
   * The singleton instance of this document send implementation.
   */
  public static final DocumentSend INSTANCE = new EmptyDocumentSend();

  /**
   * The singleton instance of the root element returned by this document send. This element has no child elements set.
   */
  private static final ObjectElement ROOT_ELEMENT = new ObjectElement(Element.NO_KEY, Set.of());

  /**
   * Sealed constructor to prevent accidental instantiations of this class. Use the singleton instance from
   * {@link #INSTANCE} instead.
   */
  private EmptyDocumentSend() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ObjectElement rootElement() {
    return ROOT_ELEMENT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable into(@NonNull DocumentFactory factory) {
    return factory.receive(this);
  }
}
