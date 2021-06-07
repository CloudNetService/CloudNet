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

package eu.cloudnetservice.cloudnet.ext.labymod.config;

import java.util.Collection;

public class LabyModConfiguration {

  private boolean enabled;
  private ServiceDisplay discordRPC;
  private DiscordJoinMatchConfig discordJoinMatch;
  private boolean discordSpectateEnabled;
  private Collection<String> excludedSpectateGroups;
  private ServiceDisplay gameModeSwitchMessages;
  private String loginDomain;
  private LabyModPermissionConfig permissionConfig;
  private LabyModBannerConfig bannerConfig;

  public LabyModConfiguration(boolean enabled, ServiceDisplay discordRPC, DiscordJoinMatchConfig discordJoinMatch,
    ServiceDisplay gameModeSwitchMessages, String loginDomain, boolean discordSpectateEnabled,
    Collection<String> excludedSpectateGroups, LabyModPermissionConfig permissionConfig,
    LabyModBannerConfig bannerConfig) {
    this.enabled = enabled;
    this.discordRPC = discordRPC;
    this.discordJoinMatch = discordJoinMatch;
    this.gameModeSwitchMessages = gameModeSwitchMessages;
    this.loginDomain = loginDomain;
    this.discordSpectateEnabled = discordSpectateEnabled;
    this.excludedSpectateGroups = excludedSpectateGroups;
    this.permissionConfig = permissionConfig;
    this.bannerConfig = bannerConfig;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public ServiceDisplay getDiscordRPC() {
    return this.discordRPC;
  }

  public void setDiscordRPC(ServiceDisplay discordRPC) {
    this.discordRPC = discordRPC;
  }

  public ServiceDisplay getGameModeSwitchMessages() {
    return this.gameModeSwitchMessages;
  }

  public void setGameModeSwitchMessages(ServiceDisplay gameModeSwitchMessages) {
    this.gameModeSwitchMessages = gameModeSwitchMessages;
  }

  public DiscordJoinMatchConfig getDiscordJoinMatch() {
    return this.discordJoinMatch;
  }

  public void setDiscordJoinMatch(DiscordJoinMatchConfig discordJoinMatch) {
    this.discordJoinMatch = discordJoinMatch;
  }

  public String getLoginDomain() {
    return this.loginDomain;
  }

  public void setLoginDomain(String loginDomain) {
    this.loginDomain = loginDomain;
  }

  public boolean isDiscordSpectateEnabled() {
    return this.discordSpectateEnabled;
  }

  public void setDiscordSpectateEnabled(boolean discordSpectateEnabled) {
    this.discordSpectateEnabled = discordSpectateEnabled;
  }

  public Collection<String> getExcludedSpectateGroups() {
    return this.excludedSpectateGroups;
  }

  public void setExcludedSpectateGroups(Collection<String> excludedSpectateGroups) {
    this.excludedSpectateGroups = excludedSpectateGroups;
  }

  public LabyModPermissionConfig getPermissionConfig() {
    return this.permissionConfig;
  }

  public void setPermissionConfig(LabyModPermissionConfig permissionConfig) {
    this.permissionConfig = permissionConfig;
  }

  public LabyModBannerConfig getBannerConfig() {
    return this.bannerConfig;
  }

  public void setBannerConfig(LabyModBannerConfig bannerConfig) {
    this.bannerConfig = bannerConfig;
  }
}
