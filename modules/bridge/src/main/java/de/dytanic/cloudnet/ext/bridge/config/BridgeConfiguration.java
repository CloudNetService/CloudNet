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

package de.dytanic.cloudnet.ext.bridge.config;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class BridgeConfiguration extends JsonDocPropertyHolder {

  public static final Map<String, Map<String, String>> DEFAULT_MESSAGES = ImmutableMap.of(
    "default",
    new HashMap<>(ImmutableMap.<String, String>builder()
      .put("command-hub-success-connect", "§7You did successfully connect to %server%")
      .put("command-hub-already-in-hub", "§cYou are already connected")
      .put("command-hub-no-server-found", "§7Hub server cannot be found")
      .put("server-join-cancel-because-maintenance", "§7This server is currently in maintenance mode")
      .put("server-join-cancel-because-permission", "§7You do not have the required permissions to join this server.")
      .put("proxy-join-cancel-because-permission", "§7You do not have the required permissions to join this proxy.")
      .put("proxy-join-cancel-because-maintenance", "§7This proxy is currently in maintenance mode")
      .put("command-cloud-sub-command-no-permission", "§7You are not allowed to use §b%command%")
      .put("already-connected", "§cYou are already connected to this network!")
      .build()));

  private final String prefix;
  private final Map<String, Map<String, String>> localizedMessages;

  private final boolean logPlayerConnections;

  private final Collection<String> excludedGroups;
  private final Collection<String> hubCommandNames;
  private final Collection<ProxyFallbackConfiguration> fallbackConfigurations;

  public BridgeConfiguration() {
    this.prefix = "&7Cloud &8| &b";
    this.localizedMessages = new HashMap<>(DEFAULT_MESSAGES);
    this.logPlayerConnections = true;
    this.excludedGroups = new ArrayList<>();
    this.hubCommandNames = Arrays.asList("hub", "lobby", "leave", "l");
    this.fallbackConfigurations = new ArrayList<>(Collections.singleton(new ProxyFallbackConfiguration(
      "Proxy",
      "Lobby",
      new ArrayList<>())));
  }

  public BridgeConfiguration(
    @NotNull String prefix,
    @NotNull Map<String, Map<String, String>> localizedMessages,
    boolean logPlayerConnections,
    @NotNull Collection<String> excludedGroups,
    @NotNull Collection<String> hubCommandNames,
    @NotNull Collection<ProxyFallbackConfiguration> fallbackConfigurations,
    @NotNull JsonDocument properties
  ) {
    this.prefix = prefix;
    this.localizedMessages = localizedMessages;
    this.logPlayerConnections = logPlayerConnections;
    this.excludedGroups = excludedGroups;
    this.hubCommandNames = hubCommandNames;
    this.fallbackConfigurations = fallbackConfigurations;
    this.properties = properties;
  }

  public @NotNull String getPrefix() {
    return this.prefix;
  }

  public @NotNull Collection<ProxyFallbackConfiguration> getFallbackConfigurations() {
    return this.fallbackConfigurations;
  }

  public @NotNull Collection<String> getHubCommandNames() {
    return this.hubCommandNames;
  }

  public @NotNull Collection<String> getExcludedGroups() {
    return this.excludedGroups;
  }

  public @NotNull String getMessage(@Nullable Locale locale, @NotNull String key) {
    return this.getMessage(locale, key, true);
  }

  public @NotNull String getMessage(@Nullable Locale locale, @NotNull String key, boolean withPrefix) {
    // try to get the messages of in the specified locale
    Map<String, String> messages = this.localizedMessages.get(locale == null ? "default" : locale.getLanguage());
    if (messages == null) {
      // get the default locale (they have to be present)
      messages = Verify.verifyNotNull(this.localizedMessages.get("default"));
    }
    // get the message from the map
    return String.format("%s%s", withPrefix ? this.prefix : "", messages.get(key));
  }
}
