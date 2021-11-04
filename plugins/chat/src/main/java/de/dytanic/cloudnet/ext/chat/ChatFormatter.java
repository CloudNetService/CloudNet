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

package de.dytanic.cloudnet.ext.chat;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public final class ChatFormatter {

  public static String buildFormat(
    @NotNull UUID playerId,
    @NotNull String playerName,
    @NotNull String displayName,
    @NotNull String format,
    @NotNull String message,
    @NotNull Function<String, Boolean> permissionTester,
    @NotNull BiFunction<Character, String, String> colorReplacer
  ) {
    PermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement().getUser(playerId);
    // check if the cloud knows a permission player
    if (permissionUser == null) {
      return null;
    }

    // check if the player is allowed to use colors and replace them
    String coloredMessage = permissionTester.apply("cloudnet.chat.color")
      ? colorReplacer.apply('&', message.replace("%", "%%"))
      : message.replace("%", "%%");
    // check if there even is a message left to prevent empty messages
    if (coloredMessage.trim().isEmpty()) {
      return null;
    }

    PermissionGroup group = CloudNetDriver.getInstance().getPermissionManagement()
      .getHighestPermissionGroup(permissionUser);
    format = format
      .replace("%name%", playerName)
      .replace("%display%", displayName)
      .replace("%uniqueId%", playerId.toString())
      .replace("%group%", group == null ? "" : group.getName())
      .replace("%display%", group == null ? "" : group.getDisplay())
      .replace("%prefix%", group == null ? "" : group.getPrefix())
      .replace("%suffix%", group == null ? "" : group.getSuffix())
      .replace("%color%", group == null ? "" : group.getColor());
    return colorReplacer.apply('&', format).replace("%message%", coloredMessage);
  }

}
