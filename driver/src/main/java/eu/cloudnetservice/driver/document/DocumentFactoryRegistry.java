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

package eu.cloudnetservice.driver.document;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A registry for document factories. Any custom document factory should get registered to this factory. By default, the
 * following standard factories are registered:
 * <ol>
 *   <li>json
 *   <li>empty
 * </ol>
 * <p>
 * This registry is primarily used when documents are transferred via the network to decode the document type on the
 * receiving network component.
 *
 * @since 4.0
 */
public interface DocumentFactoryRegistry {

  /**
   * Get an unmodifiable view of all registered document factories.
   *
   * @return an unmodifiable view of all registered document factories.
   */
  @UnmodifiableView
  @NonNull Collection<DocumentFactory> documentFactories();

  /**
   * Get the document factory with the given format name. This method never returns null and throws an exception if no
   * format with the given name is registered. Note: the format name is case-sensitive.
   *
   * @param formatName the name of the format to get.
   * @return the document factory associated with the given format name.
   * @throws NullPointerException if the given format name is null or no factory for the given format is registered.
   */
  @NonNull DocumentFactory documentFactory(@NonNull String formatName);

  /**
   * Tries to find the document factory with the given format name. This method returns null if no document factory for
   * the given format name is registered. Note: the format name is case-sensitive.
   *
   * @param formatName the name of the format to get.
   * @return the document factory associated with the given format name or null if no factory is associated.
   * @throws NullPointerException if the given format name is null.
   */
  @Nullable DocumentFactory findDocumentFactory(@NonNull String formatName);

  /**
   * Unregisters the document factory that is associated with the given format name, returning the associated factory.
   * This method returns null if no factory was associated with the given format name. Note: the format name is
   * case-sensitive.
   *
   * @param formatName the name of format to unregister.
   * @return the document factory that was associated with the given format name or null if no factory was associated.
   * @throws NullPointerException if the given format name is null.
   */
  @Nullable DocumentFactory unregisterDocumentFactory(@NonNull String formatName);

  /**
   * Associates the format name of the given document factory if no factory is already associated with the given
   * factory. Note: the format name is case-sensitive.
   *
   * @param factory the document factory to register if no mapping previously existed.
   * @throws NullPointerException if the given document factory is null.
   */
  void registerDocumentFactory(@NonNull DocumentFactory factory);

  /**
   * Associates the format name of the given document factory with the given factory. If a mapping previously existed
   * then the existing registered factory gets replaced and the method returns the old factory. Note: the format name is
   * case-sensitive.
   *
   * @param factory the document factory to register if no mapping previously existed.
   * @return the document factory that was previously associated with the format name or null if no mapping existed.
   * @throws NullPointerException if the given document factory is null.
   */
  @Nullable DocumentFactory replaceDocumentFactory(@NonNull DocumentFactory factory);
}
