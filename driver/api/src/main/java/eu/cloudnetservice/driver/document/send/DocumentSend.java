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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.send.element.ObjectElement;
import lombok.NonNull;

/**
 * Represents a version of a document that can be transferred into another document. There are predefined element types
 * that can be included in a document send:
 * <ol>
 *   <li>Objects
 *   <li>Arrays
 *   <li>Primitives
 *   <li>Null
 * </ol>
 * <p>
 * The document that is receiving a document send is not required to implement all these element types and can silently
 * ignore one when importing or not emitting one when exporting.
 *
 * @since 4.0
 */
public interface DocumentSend {

  /**
   * Get the root element of the document send. As each document is supposed to use an object element as the root
   * element this method always returns an object element.
   *
   * @return the root element of the document send.
   */
  @NonNull ObjectElement rootElement();

  /**
   * Receives this document into the given document factory. In normal cases the given document factory should create a
   * new, empty document and import all supported key-value pairs that are supplied by this send into it.
   * <p>
   * The receiving process of a document send should <strong>never</strong> throw an exception unless the given document
   * send contains malformed or invalid data making it impossible to get imported.
   *
   * @param factory the document factory that is supposed to receive this document send.
   * @return a new, empty document containing all supported key-value pairs of the given document send.
   * @throws NullPointerException if the given factory is null.
   */
  @NonNull Document.Mutable into(@NonNull DocumentFactory factory);
}
