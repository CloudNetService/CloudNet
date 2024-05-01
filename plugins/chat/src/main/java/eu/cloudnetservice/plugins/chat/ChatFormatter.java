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
import eu.cloudnetservice.ext.adventure.AdventureTextFormatLookup;
import eu.cloudnetservice.ext.component.ComponentFormats;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public final class ChatFormatter {

  private ChatFormatter() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable Component buildFormat(
    @NonNull UUID playerId,
    @NonNull String playerName,
    @NonNull Component displayName,
    @NonNull String format,
    @NonNull String message,
    @NonNull Function<String, Boolean> permissionTester,
    @NonNull PermissionManagement permissionManagement
  ) {
    var permissionUser = permissionManagement.user(playerId);
    // check if the cloud knows a permission player
    if (permissionUser == null) {
      return null;
    }

    // check if the player is allowed to use colors and replace them
    var coloredMessage = permissionTester.apply("cloudnet.chat.color")
      ? ComponentFormats.USER_INPUT.limitPlaceholders().toAdventure(message)
      : Component.text(message);

    var group = permissionManagement.highestPermissionGroup(permissionUser);

    var placeholders = new HashMap<String, Component>();
    placeholders.put("name", Component.text(playerName));
    placeholders.put("display_name", displayName);
    placeholders.put("uniqueId", Component.text(playerId.toString()));
    placeholders.put("group", Component.text(group == null ? "" : group.name()));
    placeholders.put("display", group == null ? Component.empty() : ComponentFormats.USER_INPUT.toAdventure(group.display()));
    placeholders.put("prefix", group == null ? Component.empty() : ComponentFormats.USER_INPUT.toAdventure(group.prefix()));
    placeholders.put("suffix", group == null ? Component.empty() : ComponentFormats.USER_INPUT.toAdventure(group.suffix()));

    var result = ComponentFormats.USER_INPUT
      .withPlaceholders(placeholders)
      .withPlaceholders(Map.of("message", coloredMessage));
    var groupColor = AdventureTextFormatLookup.findColor(group.color());
    if (groupColor != null) {
      result = result.withColorPlaceholder("group_color", groupColor);
    }
    return result.toAdventure(format);
  }
}
