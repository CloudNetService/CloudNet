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

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Platform;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.ResourceQueryable;

@Singleton
@PlatformPlugin(
  platform = "sponge",
  name = "CloudNet-Chat",
  version = "{project.build.version}",
  authors = "CloudNetService",
  homepage = "https://cloudnetservice.eu",
  description = "Brings chat prefixes and colored message support to all server platforms",
  dependencies = {
    @Dependency(name = "spongeapi", version = "8.0.0"),
    @Dependency(name = "CloudNet-CloudPerms", version = "{project.build.version}")
  }
)
public class SpongeChatPlugin implements PlatformEntrypoint {

  private static final MethodAccessor<Method> EVENT_CHAT_TYPE_METHOD = Reflexion
    .find("org.spongepowered.api.event.message.PlayerChatEvent$Submit")
    .flatMap(reflexion -> reflexion.findMethod("chatType"))
    .orElse(null);
  private static final MethodAccessor<Method> CANCEL_CHAT_EVENT_METHOD = Reflexion
    .find("org.spongepowered.api.event.message.PlayerChatEvent$Submit")
    .flatMap(reflexion -> reflexion.findMethod("setCancelled", boolean.class))
    .orElse(null);

  private final String chatFormat;
  private final int spongeAPIVersion;
  private final EventManager eventManager;
  private final PluginContainer pluginContainer;
  private final PermissionManagement permissionManagement;

  @Inject
  public SpongeChatPlugin(
    @NonNull ConfigManager configManager,
    @NonNull PluginManager pluginManager,
    @NonNull PluginContainer pluginContainer,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.eventManager = eventManager;
    this.pluginContainer = pluginContainer;
    this.permissionManagement = permissionManagement;

    // load the chat format
    var configPath = configManager.pluginConfig(pluginContainer).directory().resolve("config.conf");
    this.chatFormat = loadFormat(pluginContainer, configPath);

    this.spongeAPIVersion = pluginManager.plugin(Platform.API_ID)
      .map(plugin -> plugin.metadata().version().getMajorVersion())
      .orElse(0);
  }

  private static @NonNull String loadFormat(@NonNull ResourceQueryable resourceProvider, @NonNull Path configPath) {
    var loader = HoconConfigurationLoader.builder().emitComments(false).path(configPath).build();

    try {
      // check if we should copy the default config that's located inside the jar
      if (Files.notExists(configPath)) {
        // retrieve the file from the jar
        resourceProvider.openResource(URI.create("config.conf")).ifPresent(in -> {
          try (in) {
            FileUtil.copy(in, configPath);
          } catch (IOException ignored) {
          }
        });
      }

      // load the root node
      ConfigurationNode root = loader.load();
      var format = root.node("format");
      // check if the format is set
      if (format.virtual()) {
        // set defaults in config
        format.set("%display%%name% &8:&f %message%");
        loader.save(root);
      }

      return Objects.requireNonNull(format.getString());
    } catch (IOException exception) {
      throw new UncheckedIOException("Unable to load chat format for sponge", exception);
    }
  }

  @Override
  public void onLoad() {
    this.eventManager.registerListeners(this.pluginContainer, this);
  }

  @Listener(order = Order.LAST)
  public void handleAPI10(@NonNull PlayerChatEvent event, @First @NonNull ServerPlayer player) {
    // check if we are on sponge api 10 and the event is caused by a chat message submission
    if (EVENT_CHAT_TYPE_METHOD != null && event.getClass().getSimpleName().endsWith("Submit$Impl")) {
      // get the chat type using reflexion
      var chatTypeResult = EVENT_CHAT_TYPE_METHOD.<RegistryReference<?>>invoke(event)
        .map(RegistryKey::location)
        .map(ResourceKey::asString)
        .getOrElse(null);
      if (chatTypeResult != null && chatTypeResult.equals("minecraft:chat")) {
        var format = ChatFormatter.buildFormat(
          player.uniqueId(),
          player.name(),
          LegacyComponentSerializer.legacySection().serialize(player.displayName().get()),
          this.chatFormat,
          LegacyComponentSerializer.legacySection().serialize(event.message()),
          player::hasPermission,
          (colorChar, message) -> message.replace(colorChar, '§'),
          this.permissionManagement);
        // always cancel the event as we want to broadcast the message
        if (CANCEL_CHAT_EVENT_METHOD != null) {
          CANCEL_CHAT_EVENT_METHOD.invoke(event, true);
        }

        // broadcast the new message if the formatting was successful
        if (format != null) {
          Sponge.server().broadcastAudience().sendMessage(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(format));
        }
      }
    }
  }

  @Listener(order = Order.LAST)
  public void handle(@NonNull PlayerChatEvent event, @First @NonNull ServerPlayer player) {
    if (this.spongeAPIVersion < 10) {
      var format = ChatFormatter.buildFormat(
        player.uniqueId(),
        player.name(),
        LegacyComponentSerializer.legacySection().serialize(player.displayName().get()),
        this.chatFormat,
        LegacyComponentSerializer.legacySection().serialize(event.message()),
        player::hasPermission,
        (colorChar, message) -> message.replace(colorChar, '§'),
        this.permissionManagement);
      if (format == null) {
        event.setCancelled(true);
      } else {
        event.setChatFormatter(($, $1, $2, $3) -> Optional.of(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(format)));
      }
    }
  }
}
