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
import eu.cloudnetservice.ext.component.InternalPlaceholder;
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
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public final class BridgeConfiguration {

  public static final Map<String, Map<String, Component>> DEFAULT_MESSAGE_COMPONENTS = ImmutableMap.of(
    "default",
    new HashMap<>(ImmutableMap.<String, Component>builder()
      .put("command-hub-success-connect",
        Component.text("You did successfully connect to ", NamedTextColor.GRAY)
          .append(InternalPlaceholder.create("server"))
          .append(Component.text("."))
      )
      .put("command-hub-already-in-hub",
        Component.text("You are already connected to a hub service.", NamedTextColor.RED))
      .put("command-hub-no-server-found",
        Component.text("There is currently ", NamedTextColor.GRAY)
        .append(Component.text("no", NamedTextColor.RED))
        .append(Component.text("hub server available."))
      )
      .put("server-join-cancel-because-maintenance",
        Component.text("This server is currently in maintenance mode.", NamedTextColor.GRAY))
      .put("server-join-cancel-because-permission",
        Component.text("You do not have the required permissions to join this server.", NamedTextColor.GRAY)
      )
      .put("proxy-join-cancel-because-permission",
        Component.text("You do not have the required permissions to join this proxy.", NamedTextColor.GRAY))
      .put("proxy-join-cancel-because-maintenance", Component.text("This proxy is currently in maintenance mode.", NamedTextColor.GRAY))
      .put("proxy-join-disconnect-because-no-hub",
        Component.text("There is currently no hub server you can connect to.", NamedTextColor.RED))
      .put("command-cloud-sub-command-no-permission",
        Component.text("You are not allowed to use ", NamedTextColor.GRAY)
        .append(InternalPlaceholder.create("command").color(NamedTextColor.AQUA))
        .append(Component.text("."))
      )
      .put("already-connected",
        Component.text("You are already connected to this network!", NamedTextColor.RED))
      .put("error-connecting-to-server",
        Component.text("Unable to connect to ", NamedTextColor.RED)
        .append(InternalPlaceholder.create("server"))
        .append(Component.text(": "))
        .append(InternalPlaceholder.create("reason"))
      )
      .build()));

  public static final Map<String, Map<String, String>> DEFAULT_MESSAGES;

  static {
    var map = new HashMap<String, Map<String, String>>();
    for (var language : DEFAULT_MESSAGE_COMPONENTS.entrySet()) {
      var messages = new HashMap<String, String>();
      for (var message : language.getValue().entrySet()) {
        messages.put(
          message.getKey(),
          InternalPlaceholder.replacePlaceholders(
            ComponentFormats.USER_INPUT.fromAdventure(message.getValue())
          )
        );
      }
      map.put(language.getKey(), messages);
    }
    DEFAULT_MESSAGES = ImmutableMap.copyOf(map);
  }

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
