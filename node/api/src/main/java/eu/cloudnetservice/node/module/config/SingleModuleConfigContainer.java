/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module.config;

import eu.cloudnetservice.driver.document.Document;
import java.util.function.Consumer;
import lombok.NonNull;

public non-sealed interface SingleModuleConfigContainer<T> extends ModuleConfigContainer<T> {

  /**
   * Updates the underlying configuration model from the given document.
   *
   * @param document the document containing the configuration model to apply.
   * @throws NullPointerException     if the given document is null.
   * @throws IllegalArgumentException if the configuration in the given document is invalid.
   */
  void updateFromDocument(@NonNull Document document);

  /**
   * Registers a listener which will be triggered if this configuration container changes, either by being reloaded or
   * by being updated.
   *
   * @param listener the update listener to invoke when the underlying configuration changes.
   * @throws NullPointerException if the given listener is null.
   */
  void registerUpdateListener(@NonNull Consumer<T> listener);

  /**
   * Get the loaded and modeled configuration instance.
   *
   * @return the loaded and modeled configuration instance.
   */
  @NonNull
  T configModel();
}
