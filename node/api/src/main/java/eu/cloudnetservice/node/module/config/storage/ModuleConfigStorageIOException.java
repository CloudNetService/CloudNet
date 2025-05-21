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

import java.io.Serial;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Signals that a load/store error of some sort has occurred in a module configuration storage.
 *
 * @since 4.0
 */
public final class ModuleConfigStorageIOException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -1681128787864765942L;

  /**
   * Constructs a new storage I/O exception with the given detail message.
   *
   * @param message the detail message.
   * @throws NullPointerException if the given detail message is null.
   */
  public ModuleConfigStorageIOException(@NonNull String message) {
    super(message);
  }

  /**
   * Constructs a new storage I/O exception with the given detail message and optional original cause.
   *
   * @param message the detail message.
   * @param cause   the optional cause for this exception.
   * @throws NullPointerException if the given detail message is null.
   */
  public ModuleConfigStorageIOException(@NonNull String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
