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

import com.google.inject.Inject;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_chat")
public class SpongeChatPlugin {

  private static final Logger LOGGER = LogManager.logger(SpongeChatPlugin.class);

  private final Path configFilePath;
  private volatile String chatFormat;

  @Inject
  public SpongeChatPlugin(@ConfigDir(sharedRoot = false) @NonNull Path configDirectory) {
    this.configFilePath = configDirectory.resolve("config.conf");
  }

  @Listener
  public void handle(@NonNull ConstructPluginEvent event) {
    ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
      .path(this.configFilePath)
      .build();
    try {
      // load the root node
      ConfigurationNode root = loader.load();
      var format = root.node("format");
      // check if the format is set
      if (format.virtual()) {
        // set defaults in config
        format.set("%display%%name% &8:&f %message%");
        loader.save(root);
      }

      this.chatFormat = format.getString();
    } catch (IOException exception) {
      LOGGER.severe("Exception while creating a file", exception);
    }
  }

  @Listener
  public void handle(@NonNull PlayerChatEvent event, @First @NonNull ServerPlayer player) {
    var format = ChatFormatter.buildFormat(
      player.uniqueId(),
      player.name(),
      LegacyComponentSerializer.legacySection().serialize(player.displayName().get()),
      this.chatFormat,
      LegacyComponentSerializer.legacySection().serialize(event.message()),
      player::hasPermission,
      (colorChar, message) -> message.replace(colorChar, '§'));
    if (format == null) {
      event.setCancelled(true);
    } else {
      event.setChatFormatter(($, $1, $2, $3) -> Optional.of(AdventureSerializerUtil.serialize(format)));
    }
  }
}
