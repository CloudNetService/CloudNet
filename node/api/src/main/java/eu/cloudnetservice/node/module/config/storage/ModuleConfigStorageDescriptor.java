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

package eu.cloudnetservice.node.module.config.storage;

import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Holder for descriptive information about a module storage.
 *
 * @since 4.0
 */
public interface ModuleConfigStorageDescriptor {

  /**
   * Get the name of the storage to which this descriptor belongs.
   *
   * @return the name of the storage to which this descriptor belongs.
   */
  @NonNull
  String storageName();

  /**
   * Get an unmodifiable view of all flags that are set on the associated configuration storage.
   *
   * @return all flags that are set on the associated configuration storage.
   */
  @NonNull
  @Unmodifiable
  List<ModuleConfigStorageFlag> flags();

  /**
   * Get if the given flag is set on the associated configuration storage.
   *
   * @param flag the flag to check.
   * @return true if the given flag is set on the associated module storage, false otherwise.
   * @throws NullPointerException if the given flag is null.
   */
  boolean flagSet(@NonNull ModuleConfigStorageFlag flag);
}
