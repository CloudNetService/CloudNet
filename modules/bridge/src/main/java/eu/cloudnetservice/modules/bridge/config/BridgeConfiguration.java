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

package eu.cloudnetservice.modules.bridge.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.document.property.JsonDocPropertyHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class BridgeConfiguration extends JsonDocPropertyHolder {

  public static final Map<String, Map<String, String>> DEFAULT_MESSAGES = ImmutableMap.of(
    "default",
    new HashMap<>(ImmutableMap.<String, String>builder()
      .put("command-hub-success-connect", "§7You did successfully connect to %server%.")
      .put("command-hub-already-in-hub", "§cYou are already connected to a hub service.")
      .put("command-hub-no-server-found", "§7The is currently §cno §7hub server available.")
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
    super(JsonDocument.newDocument());
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
    @NonNull Collection<ProxyFallbackConfiguration> fallbackConfigurations,
    @NonNull JsonDocument properties
  ) {
    super(properties);
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

  public @NonNull String message(@Nullable Locale locale, @NonNull String key) {
    return this.message(locale, key, true);
  }

  public @NonNull String message(@Nullable Locale locale, @NonNull String key, boolean withPrefix) {
    // try to get the messages of in the specified locale
    var messages = this.localizedMessages.get(locale == null ? "default" : locale.getLanguage());
    if (messages == null) {
      // get the default locale (they have to be present)
      messages = Preconditions.checkNotNull(this.localizedMessages.get("default"));
    }
    // get the message from the map
    return String.format(
      "%s%s",
      withPrefix ? this.prefix : "",
      messages.getOrDefault(key, DEFAULT_MESSAGES.get("default").get(key)));
  }
}
