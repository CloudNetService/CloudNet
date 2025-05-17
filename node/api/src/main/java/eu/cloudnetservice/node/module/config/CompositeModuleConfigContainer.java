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

import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <T>
 */
public non-sealed interface CompositeModuleConfigContainer<T extends IdentifiableModuleConfig>
  extends ModuleConfigContainer<T>, Iterable<T> {

  void removeConfiguration(@NonNull T configuration);

  void removeConfiguration(@NonNull String configurationId);

  @Nullable
  T configuration(@NonNull String configurationId);

  /**
   * @return
   */
  List<T> loadedConfigurations();

  /**
   * Returns a sequential stream holding all the configuration models loaded in this container.
   *
   * @return
   */
  Stream<T> loadedConfigurationsStream();
}
