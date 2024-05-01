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

package eu.cloudnetservice.modules.bridge.config;

import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.ext.component.ComponentFormat;
import eu.cloudnetservice.ext.component.ComponentFormats;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public final class BridgeConfiguration {

  public static final Map<String, Map<String, String>> DEFAULT_MESSAGES = ImmutableMap.of(
    "default",
    new HashMap<>(ImmutableMap.<String, String>builder()
      .put("command-hub-success-connect", "<gray>You did successfully connect to <server>.</gray>")
      .put("command-hub-already-in-hub", "<red>You are already connected to a hub service.</red>")
      .put("command-hub-no-server-found", "<gray>There is currently</gray> <red>no</red> <gray>hub server available.</gray>")
      .put("server-join-cancel-because-maintenance", "<gray>This server is currently in maintenance mode.</gray>")
      .put("server-join-cancel-because-permission", "<gray>You do not have the required permissions to join this server.</gray>")
      .put("proxy-join-cancel-because-permission", "<gray>You do not have the required permissions to join this proxy.</gray>")
      .put("proxy-join-cancel-because-maintenance", "<gray>This proxy is currently in maintenance mode.</gray>")
      .put("proxy-join-disconnect-because-no-hub", "<red>There is currently no hub server you can connect to.</red>")
      .put("command-cloud-sub-command-no-permission", "<gray>You are not allowed to use</gray> <aqua><command></aqua><gray>.</gray>")
      .put("already-connected", "<red>You are already connected to this network!</red>")
      .put("error-connecting-to-server", "<red>Unable to connect to <server>: <reason></red>")
      .build()));

  private final String prefix;
  private final Map<String, Map<String, String>> localizedMessages;

  private final Collection<String> excludedGroups;
  private final Collection<String> hubCommandNames;
  private final Collection<ProxyFallbackConfiguration> fallbackConfigurations;

  public BridgeConfiguration() {
    this.prefix = "<gray>Cloud</gray> <dark_gray>|</dark_gray> ";
    this.localizedMessages = new HashMap<>(DEFAULT_MESSAGES);
    this.excludedGroups = new ArrayList<>();
    this.hubCommandNames = Arrays.asList("hub", "lobby", "leave", "l");
    this.fallbackConfigurations = new ArrayList<>(List.of(ProxyFallbackConfiguration.builder()
      .targetGroup("Proxy")
      .defaultFallbackTask("Lobby")
      .build()));
  }

  public BridgeConfiguration(
    @NonNull String prefix,
    @NonNull Map<String, Map<String, String>> localizedMessages,
    @NonNull Collection<String> excludedGroups,
    @NonNull Collection<String> hubCommandNames,
    @NonNull Collection<ProxyFallbackConfiguration> fallbackConfigurations
  ) {
    this.prefix = prefix;
    this.localizedMessages = localizedMessages;
    this.excludedGroups = excludedGroups;
    this.hubCommandNames = hubCommandNames;
    this.fallbackConfigurations = fallbackConfigurations;
  }

  public @NonNull Component prefix() {
    return ComponentFormats.USER_INPUT.toAdventure(this.prefix);
  }

  public @NonNull Collection<ProxyFallbackConfiguration> fallbackConfigurations() {
    return this.fallbackConfigurations;
  }

  public @NonNull Collection<String> hubCommandNames() {
    return this.hubCommandNames;
  }

  public @NonNull Collection<String> excludedGroups() {
    return this.excludedGroups;
  }

  public void handleMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull Consumer<Component> sender
  ) {
    this.handleMessage(locale, key, ComponentFormats.ADVENTURE, sender);
  }

  public <C> void handleMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull ComponentFormat<C> format,
    @NonNull Consumer<C> sender
  ) {
    this.handleMessage(locale, key, format, sender, true);
  }

  public <C> void handleMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull ComponentFormat<C> format,
    @NonNull Consumer<C> sender,
    boolean withPrefix
  ) {
    this.handleMessage(locale, key, format, sender, withPrefix, Map.of());
  }

  public <C> void handleMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull ComponentFormat<C> format,
    @NonNull Consumer<C> sender,
    boolean withPrefix,
    @NonNull Map<String, Component> placeholders
  ) {
    C component = this.findMessage(locale, key, format, null, withPrefix, placeholders);
    if (component != null) {
      sender.accept(component);
    }
  }

  public @UnknownNullability <C> C findMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull ComponentFormat<C> format,
    @Nullable C defaultValue,
    boolean withPrefix
  ) {
    return this.findMessage(locale, key, format, defaultValue, withPrefix, Map.of());
  }

  public @UnknownNullability <C> C findMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull ComponentFormat<C> format,
    @Nullable C defaultValue,
    boolean withPrefix,
    @NonNull Map<String, Component> placeholders
  ) {
    String message = null;
    // don't bother resolving if no locale is present
    if (locale != null) {
      // try to get the messages of the specified locale, with country
      message = this.resolveMessage(this.localizedMessages.get(locale.toString()), key);
      if (message == null) {
        // try to resolve the messages without country
        message = this.resolveMessage(this.localizedMessages.get(locale.getLanguage()), key);
      }
    }

    // check if the message is available for the "default" locale
    if (message == null) {
      message = this.resolveMessage(this.localizedMessages.get("default"), key);
      if (message == null) {
        return defaultValue;
      }
    }

    // format the final message
    var formattedMessage = Component.empty();
    if (withPrefix) {
      formattedMessage.append(this.prefix());
    }
    formattedMessage.append(ComponentFormats.USER_INPUT.withPlaceholders(placeholders).toAdventure(message));
    C component = format.fromAdventure(formattedMessage);

    // check if the converter was able to convert the message
    return component;
  }

  private @Nullable String resolveMessage(@Nullable Map<String, String> messages, @NonNull String key) {
    return messages == null ? null : messages.get(key);
  }
}
