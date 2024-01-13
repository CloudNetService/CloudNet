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

package eu.cloudnetservice.plugins.chat;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class ChatFormatter {

  private ChatFormatter() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable String buildFormat(
    @NonNull UUID playerId,
    @NonNull String playerName,
    @NonNull String displayName,
    @NonNull String format,
    @NonNull String message,
    @NonNull Function<String, Boolean> permissionTester,
    @NonNull BiFunction<Character, String, String> colorReplacer,
    @NonNull PermissionManagement permissionManagement
  ) {
    var permissionUser = permissionManagement.user(playerId);
    // check if the cloud knows a permission player
    if (permissionUser == null) {
      return null;
    }

    // check if the player is allowed to use colors and replace them
    var coloredMessage = permissionTester.apply("cloudnet.chat.color")
      ? colorReplacer.apply('&', message.replace("%", "%%"))
      : message.replace("%", "%%");
    // check if there even is a message left to prevent empty messages
    if (coloredMessage.trim().isEmpty()) {
      return null;
    }

    var group = permissionManagement.highestPermissionGroup(permissionUser);
    format = format
      .replace("%name%", playerName)
      .replace("%display_name%", displayName)
      .replace("%uniqueId%", playerId.toString())
      .replace("%group%", group == null ? "" : group.name())
      .replace("%display%", group == null ? "" : group.display())
      .replace("%prefix%", group == null ? "" : group.prefix())
      .replace("%suffix%", group == null ? "" : group.suffix())
      .replace("%color%", group == null ? "" : group.color());
    return colorReplacer.apply('&', format).replace("%message%", coloredMessage);
  }
}
