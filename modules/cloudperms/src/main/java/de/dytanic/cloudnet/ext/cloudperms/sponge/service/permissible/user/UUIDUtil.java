/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible.user;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class UUIDUtil {

  private UUIDUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull UUID parseFromString(@NotNull String id) {
    UUID result = parseFromStringOrNull(id);
    Objects.requireNonNull(result, "Identifier \"" + id + "\" is not a valid unique id");
    return result;
  }

  public static @Nullable UUID parseFromStringOrNull(@NotNull String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException exception) {
      // might be an un-dashed unique id
      if (id.length() == 32) {
        try {
          return new UUID(
            Long.parseUnsignedLong(id.substring(0, 16), 16),
            Long.parseUnsignedLong(id.substring(16), 16));
        } catch (NumberFormatException ignored) {
        }
      }
    }
    // unable to parsere
    return null;
  }
}
