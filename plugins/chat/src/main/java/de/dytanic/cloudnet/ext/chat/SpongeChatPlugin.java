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

import com.google.inject.Inject;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

@Plugin(
  id = "cloudnet_chat",
  name = "CloudNet-Chat",
  version = "1.0",
  url = "https://cloudnetservice.eu"
)
public class SpongeChatPlugin {

  private static final Logger LOGGER = LogManager.getLogger(SpongeChatPlugin.class);

  @Inject
  @DefaultConfig(sharedRoot = false)
  public Path defaultConfigPath;

  private String chatFormat;

  @Listener
  public void handle(GameInitializationEvent event) {
    ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
      .setPath(this.defaultConfigPath)
      .build();
    try {
      CommentedConfigurationNode configurationNode = loader.load();
      if (Files.notExists(this.defaultConfigPath)) {
        Files.createFile(this.defaultConfigPath);
        // set defaults in config
        configurationNode.getNode("format").setValue("%display%%name% &8:&f %message%");
        loader.save(configurationNode);
      }

      CommentedConfigurationNode format = configurationNode.getNode("format");
      if (format.isVirtual()) {
        // the node is not set in the config/was removed
        format.setValue("%display%%name% &8:&f %message%");
        loader.save(configurationNode);
      }

      this.chatFormat = format.getString();
    } catch (IOException exception) {
      LOGGER.severe("Exception while creating a file", exception);
    }
  }

  @Listener
  public void handle(MessageChannelEvent.Chat event) {
    event.getCause().first(Player.class).ifPresent(player -> {
      String format = ChatFormatter.buildFormat(
        player.getUniqueId(),
        player.getName(),
        player.getDisplayNameData().displayName().get().toPlain(),
        this.chatFormat,
        event.getRawMessage().toPlain(),
        player::hasPermission,
        (colorChar, message) -> TextSerializers.FORMATTING_CODE.replaceCodes(message, colorChar)
      );
      if (format == null) {
        event.setCancelled(true);
      } else {
        event.setMessage(Text.of(format));
      }
    });
  }

}
