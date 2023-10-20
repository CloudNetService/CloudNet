/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.extensions.Extension;

@Singleton
@PlatformPlugin(
  platform = "minestom",
  name = "CloudNet-Chat",
  authors = "CloudNetService",
  version = "@version@",
  dependencies = @Dependency(name = "CloudNet-CloudPerms")
)
public class MinestomChatExtension implements PlatformEntrypoint {

  private final String format;
  private final PermissionManagement permissionManagement;

  @Inject
  public MinestomChatExtension(@NonNull Extension extension, @NonNull PermissionManagement permissionManagement) {
    try {
      this.format = readFormat(extension);
      this.permissionManagement = permissionManagement;
    } catch (IOException exception) {
      throw new UncheckedIOException("Unable to read cloudnet-chat format", exception);
    }
  }

  private static @NonNull String readFormat(@NonNull Extension extension) throws IOException {
    try (var stream = extension.getResource("config.properties")) {
      var properties = new Properties();
      properties.load(stream);
      return properties.getProperty("format");
    }
  }

  @Override
  public void onLoad() {
    var node = EventNode.type("cloudnet-chat", EventFilter.PLAYER);
    MinecraftServer.getGlobalEventHandler().addChild(node.addListener(PlayerChatEvent.class, this::handleChat));
  }

  private void handleChat(@NonNull PlayerChatEvent event) {
    // ignore fake players
    var player = event.getPlayer();
    if (player instanceof FakePlayer) {
      return;
    }

    var format = ChatFormatter.buildFormat(
      player.getUuid(),
      player.getUsername(),
      LegacyComponentSerializer.legacySection().serialize(player.getName()),
      this.format,
      event.getMessage(),
      player::hasPermission,
      (character, message) -> message.replace(character, '§'),
      this.permissionManagement);
    if (format == null) {
      event.setCancelled(true);
    } else {
      event.setChatFormat($ -> ComponentFormats.BUNGEE_TO_ADVENTURE.convert(format));
    }
  }
}
