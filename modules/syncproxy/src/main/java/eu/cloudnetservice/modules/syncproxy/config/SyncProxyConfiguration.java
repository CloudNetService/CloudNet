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

package eu.cloudnetservice.modules.syncproxy.config;

import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.ext.component.InternalPlaceholder;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConstants;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public record SyncProxyConfiguration(
  @Unmodifiable @NonNull Set<SyncProxyLoginConfiguration> loginConfigurations,
  @Unmodifiable @NonNull Set<SyncProxyTabListConfiguration> tabListConfigurations,
  @Unmodifiable @NonNull Map<String, String> messages,
  boolean ingameServiceStartStopMessages
) {

  public static final Map<String, Component> DEFAULT_MESSAGE_COMPONENTS = ImmutableMap.of(
    "player-login-not-whitelisted",
      Component.text("The network is currently in maintenance!", NamedTextColor.RED),
    "player-login-full-server",
      Component.text("The network is currently full. You need extra permissions to enter the network", NamedTextColor.RED),
    "service-start",
      Component.text("The service ", NamedTextColor.GRAY)
        .append(InternalPlaceholder.create("service").color(NamedTextColor.YELLOW))
        .append(Component.text(" is "))
        .append(Component.text("starting", NamedTextColor.GREEN))
        .append(Component.text(" on node "))
        .append(InternalPlaceholder.create("node").color(NamedTextColor.YELLOW))
        .append(Component.text("...")),
    "service-stop",
      Component.text("The service ")
        .append(InternalPlaceholder.create("service").color(NamedTextColor.YELLOW))
        .append(Component.text(" is "))
        .append(Component.text("stopping", NamedTextColor.RED))
        .append(Component.text(" on node "))
        .append(InternalPlaceholder.create("node").color(NamedTextColor.YELLOW))
        .append(Component.text("..."))
  );

  public static final Map<String, String> DEFAULT_MESSAGES;

  static {
    var map = new HashMap<String, String>();
    for (var message : DEFAULT_MESSAGE_COMPONENTS.entrySet()) {
        map.put(
          message.getKey(),
          InternalPlaceholder.process(
            ComponentFormats.USER_INPUT.fromAdventure(message.getValue())
          )
        );
      }
    DEFAULT_MESSAGES = ImmutableMap.copyOf(map);
  }

  public static void fillCommonPlaceholders(
    @NonNull Map<String, Component> map,
    @NonNull ServiceInfoSnapshot serviceInfoSnapshot,
    int onlinePlayers,
    int maxPlayers
  ) {
    map.put("online_players", Component.text(onlinePlayers));
    map.put("max_players", Component.text(maxPlayers));
    BridgeServiceHelper.fillCommonPlaceholders(map, null, serviceInfoSnapshot);
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SyncProxyConfiguration configuration) {
    return builder()
      .loginConfigurations(configuration.loginConfigurations())
      .tabListConfigurations(configuration.tabListConfigurations())
      .messages(configuration.messages())
      .ingameStartStopMessages(configuration.ingameServiceStartStopMessages());
  }

  public static @NonNull SyncProxyConfiguration createDefault(@NonNull String targetGroup) {
    return builder()
      .modifyLoginConfigurations(login -> login.add(SyncProxyLoginConfiguration.createDefault(targetGroup)))
      .modifyTabListConfigurations(tabList -> tabList.add(SyncProxyTabListConfiguration.createDefault(targetGroup)))
      .messages(DEFAULT_MESSAGES)
      .ingameStartStopMessages(true)
      .build();
  }

  public static @Nullable SyncProxyConfiguration configurationFromNode(@NonNull String nodeUniqueId) {
    var response = ChannelMessage.builder()
      .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL)
      .message(SyncProxyConstants.SYNC_PROXY_CONFIG_REQUEST)
      .targetNode(nodeUniqueId)
      .build()
      .sendSingleQuery();

    if (response != null) {
      return response.content().readObject(SyncProxyConfiguration.class);
    }

    return null;
  }

  public @NonNull Component message(@NonNull String key) {
    return this.message(key, Map.of());
  }

  public @NonNull Component message(@NonNull String key, @NonNull Map<String, Component> placeholders) {
    var message = this.messages.getOrDefault(key, DEFAULT_MESSAGES.get(key));
    return ComponentFormats.USER_INPUT.withPlaceholders(placeholders).toAdventure(message);
  }

  public void sendUpdate() {
    ChannelMessage.builder()
      .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL)
      .message(SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIG)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(this))
      .build()
      .send();
  }

  public static class Builder {

    private Set<SyncProxyLoginConfiguration> loginConfigurations = new HashSet<>();
    private Set<SyncProxyTabListConfiguration> tabListConfigurations = new HashSet<>();
    private Map<String, String> messages = new HashMap<>();
    private boolean ingameServiceStartStopMessages;

    public @NonNull Builder loginConfigurations(@NonNull Set<SyncProxyLoginConfiguration> configurations) {
      this.loginConfigurations = new HashSet<>(configurations);
      return this;
    }

    public @NonNull Builder modifyLoginConfigurations(@NonNull Consumer<Set<SyncProxyLoginConfiguration>> modifier) {
      modifier.accept(this.loginConfigurations);
      return this;
    }

    public @NonNull Builder tabListConfigurations(@NonNull Set<SyncProxyTabListConfiguration> configurations) {
      this.tabListConfigurations = new HashSet<>(configurations);
      return this;
    }

    public @NonNull Builder modifyTabListConfigurations(
      @NonNull Consumer<Set<SyncProxyTabListConfiguration>> modifier) {
      modifier.accept(this.tabListConfigurations);
      return this;
    }

    public @NonNull Builder messages(@NonNull Map<String, String> messages) {
      this.messages = new HashMap<>(messages);
      return this;
    }

    public @NonNull Builder modifyMessages(@NonNull Consumer<Map<String, String>> modifier) {
      modifier.accept(this.messages);
      return this;
    }

    public @NonNull Builder ingameStartStopMessages(boolean ingameMessages) {
      this.ingameServiceStartStopMessages = ingameMessages;
      return this;
    }

    public @NonNull SyncProxyConfiguration build() {
      return new SyncProxyConfiguration(
        Set.copyOf(this.loginConfigurations),
        Set.copyOf(this.tabListConfigurations),
        Map.copyOf(this.messages),
        this.ingameServiceStartStopMessages);
    }
  }
}
