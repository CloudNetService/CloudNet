/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
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

public class MinestomChatExtension extends Extension {

  private String format;

  @Override
  public void initialize() {
    try {
      this.format = this.readFormat();
    } catch (IOException exception) {
      throw new UncheckedIOException("Unable to read cloudnet-chat format", exception);
    }

    var node = EventNode.type("cloudnet-chat", EventFilter.PLAYER);
    MinecraftServer.getGlobalEventHandler().addChild(node.addListener(PlayerChatEvent.class, this::handleChat));
  }

  @Override
  public void terminate() {

  }

  private void handleChat(@NonNull PlayerChatEvent event) {
    var player = event.getPlayer();
    // ignore fake players
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
      (character, message) -> message.replace(character, '§'));
    if (format == null) {
      event.setCancelled(true);
    } else {
      event.setChatFormat($ -> AdventureSerializerUtil.serialize(format));
    }
  }

  private @NonNull String readFormat() throws IOException {
    var config = this.getResource("config.properties");
    var properties = new Properties();
    properties.load(config);
    return properties.getProperty("format");
  }
}
