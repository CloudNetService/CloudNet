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

package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class BridgeConfiguration extends BasicJsonDocPropertyable {

  public static final Type TYPE = new TypeToken<BridgeConfiguration>() {
  }.getType();
  public static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

  static {
    DEFAULT_MESSAGES.put("command-hub-success-connect", "§7You did successfully connect to %server%");
    DEFAULT_MESSAGES.put("command-hub-already-in-hub", "§cYou are already connected");
    DEFAULT_MESSAGES.put("command-hub-no-server-found", "§7Hub server cannot be found");
    DEFAULT_MESSAGES.put("server-join-cancel-because-only-proxy", "§7You must connect from an original proxy server");
    DEFAULT_MESSAGES.put("server-join-cancel-because-maintenance", "§7This server is currently in maintenance mode");
    DEFAULT_MESSAGES.put("server-join-cancel-because-permission",
      "§7You do not have the required permissions to connect to this server.");
    DEFAULT_MESSAGES.put("command-cloud-sub-command-no-permission", "§7You are not allowed to use §b%command%");
    DEFAULT_MESSAGES.put("already-connected", "§cYou are already connected to this network!");
  }

  private String prefix = "&7Cloud &8| &b";

  private boolean onlyProxyProtection = true;

  private Collection<String> excludedOnlyProxyWalkableGroups = new ArrayList<>();

  private Collection<String> excludedGroups = new ArrayList<>();

  private Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations = new ArrayList<>();

  private Collection<String> hubCommandNames = Arrays.asList("hub", "lobby", "leave", "l");

  private boolean logPlayerConnections = true;

  private Map<String, String> messages = DEFAULT_MESSAGES;

  public BridgeConfiguration(String prefix, boolean onlyProxyProtection,
    Collection<String> excludedOnlyProxyWalkableGroups, Collection<String> excludedGroups,
    Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations, Map<String, String> messages,
    boolean logPlayerConnections) {
    this.prefix = prefix;
    this.onlyProxyProtection = onlyProxyProtection;
    this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
    this.excludedGroups = excludedGroups;
    this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
    this.logPlayerConnections = logPlayerConnections;
    this.messages = messages;
  }

  public BridgeConfiguration(String prefix, boolean onlyProxyProtection,
    Collection<String> excludedOnlyProxyWalkableGroups, Collection<String> excludedGroups,
    Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations, Collection<String> hubCommandNames,
    boolean logPlayerConnections, Map<String, String> messages) {
    this.prefix = prefix;
    this.onlyProxyProtection = onlyProxyProtection;
    this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
    this.excludedGroups = excludedGroups;
    this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
    this.hubCommandNames = hubCommandNames;
    this.logPlayerConnections = logPlayerConnections;
    this.messages = messages;
  }

  public BridgeConfiguration() {
  }

  public String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public boolean isOnlyProxyProtection() {
    return this.onlyProxyProtection;
  }

  public void setOnlyProxyProtection(boolean onlyProxyProtection) {
    this.onlyProxyProtection = onlyProxyProtection;
  }

  public Collection<String> getExcludedOnlyProxyWalkableGroups() {
    return this.excludedOnlyProxyWalkableGroups;
  }

  public void setExcludedOnlyProxyWalkableGroups(Collection<String> excludedOnlyProxyWalkableGroups) {
    this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
  }

  public Collection<String> getExcludedGroups() {
    return this.excludedGroups;
  }

  public void setExcludedGroups(Collection<String> excludedGroups) {
    this.excludedGroups = excludedGroups;
  }

  public Collection<ProxyFallbackConfiguration> getBungeeFallbackConfigurations() {
    return this.bungeeFallbackConfigurations;
  }

  public void setBungeeFallbackConfigurations(Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations) {
    this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
  }

  public Map<String, String> getMessages() {
    return this.messages;
  }

  public void setMessages(Map<String, String> messages) {
    this.messages = messages;
  }

  public boolean isLogPlayerConnections() {
    return this.logPlayerConnections;
  }

  public void setLogPlayerConnections(boolean logPlayerConnections) {
    this.logPlayerConnections = logPlayerConnections;
  }

  public Collection<String> getHubCommandNames() {
    return this.hubCommandNames;
  }

}
