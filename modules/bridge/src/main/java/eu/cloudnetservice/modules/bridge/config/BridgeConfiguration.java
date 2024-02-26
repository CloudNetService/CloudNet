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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public final class BridgeConfiguration {

  public static final Map<String, Map<String, String>> DEFAULT_MESSAGES = ImmutableMap.of(
    "default",
    new HashMap<>(ImmutableMap.<String, String>builder()
      .put("command-hub-success-connect", "§7You did successfully connect to %server%.")
      .put("command-hub-already-in-hub", "§cYou are already connected to a hub service.")
      .put("command-hub-no-server-found", "§7There is currently §cno §7hub server available.")
      .put("server-join-cancel-because-maintenance", "§7This server is currently in maintenance mode.")
      .put("server-join-cancel-because-permission", "§7You do not have the required permissions to join this server.")
      .put("proxy-join-cancel-because-permission", "§7You do not have the required permissions to join this proxy.")
      .put("proxy-join-cancel-because-maintenance", "§7This proxy is currently in maintenance mode.")
      .put("proxy-join-disconnect-because-no-hub", "§cThere is currently no hub server you can connect to.")
      .put("command-cloud-sub-command-no-permission", "§7You are not allowed to use §b%command%.")
      .put("already-connected", "§cYou are already connected to this network!")
      .put("error-connecting-to-server", "§cUnable to connect to %server%: %reason%")
      .build()));

  private final String prefix;
  private final Map<String, Map<String, String>> localizedMessages;

  private final Collection<String> excludedGroups;
  private final Collection<String> hubCommandNames;
  private final Collection<ProxyFallbackConfiguration> fallbackConfigurations;

  public BridgeConfiguration() {
    this.prefix = "§7Cloud §8| §b";
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

  public @NonNull String prefix() {
    return this.prefix;
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
    @NonNull Consumer<String> sender
  ) {
    this.handleMessage(locale, key, Function.identity(), sender, true);
  }

  public <C> void handleMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull Function<String, C> toComponentConverter,
    @NonNull Consumer<C> sender
  ) {
    this.handleMessage(locale, key, toComponentConverter, sender, true);
  }

  public <C> void handleMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull Function<String, C> toComponentConverter,
    @NonNull Consumer<C> sender,
    boolean withPrefix
  ) {
    C component = this.findMessage(locale, key, toComponentConverter, null, withPrefix);
    if (component != null) {
      sender.accept(component);
    }
  }

  public @UnknownNullability <C> C findMessage(
    @Nullable Locale locale,
    @NonNull String key,
    @NonNull Function<String, C> toComponentConverter,
    @Nullable C defaultValue,
    boolean withPrefix
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
    var formattedMessage = String.format("%s%s", withPrefix ? this.prefix : "", message);
    C component = toComponentConverter.apply(formattedMessage);

    // check if the converter was able to convert the message
    return component != null ? component : defaultValue;
  }

  private @Nullable String resolveMessage(@Nullable Map<String, String> messages, @NonNull String key) {
    return messages == null ? null : messages.get(key);
  }
}
