/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.config;

import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageDescriptor;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageFlag;
import java.util.List;
import lombok.NonNull;

/**
 * Default implementation of a storage descriptor.
 *
 * @param storageName the name of the storage.
 * @param flags       the flags set on the associated storage.
 * @since 4.0
 */
public record DefaultModuleConfigStorageDescriptor(
  @NonNull String storageName,
  @NonNull List<ModuleConfigStorageFlag> flags
) implements ModuleConfigStorageDescriptor {

  public DefaultModuleConfigStorageDescriptor {
    flags = List.copyOf(flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean flagSet(@NonNull ModuleConfigStorageFlag flag) {
    return this.flags.contains(flag);
  }
}
