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
import eu.cloudnetservice.ext.component.MinimessageUtils;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
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
      ? MiniMessage.builder()
      .tags(TagResolver.resolver(
        StandardTags.color(),
        StandardTags.decorations(),
        StandardTags.gradient(), StandardTags.rainbow()
      )).build().deserialize(message)
      : Component.text(message);

    var group = permissionManagement.highestPermissionGroup(permissionUser);

    var placeholders = new HashMap<String, Component>();
    placeholders.put("name", Component.text(playerName));
    placeholders.put("display_name", displayName);
    placeholders.put("uniqueId", Component.text(playerId.toString()));
    placeholders.put("group", Component.text(group == null ? "" : group.name()));
    placeholders.put("display", group == null ? Component.empty() : MiniMessage.miniMessage().deserialize(group.display()));
    placeholders.put("prefix", group == null ? Component.empty() : MiniMessage.miniMessage().deserialize(group.prefix()));
    placeholders.put("suffix", group == null ? Component.empty() : MiniMessage.miniMessage().deserialize(group.suffix()));
    placeholders.put("color", group == null ? Component.empty() : MiniMessage.miniMessage().deserialize(group.color()));

    return MiniMessage.miniMessage()
      .deserialize(format,
        TagResolver.resolver(MinimessageUtils.tagsFromMap(placeholders)),
        Placeholder.component("message", coloredMessage)
      );
  }
}
