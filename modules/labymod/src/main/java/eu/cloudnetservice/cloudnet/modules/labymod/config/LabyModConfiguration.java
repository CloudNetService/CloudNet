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

package eu.cloudnetservice.cloudnet.modules.labymod.config;

import com.google.common.base.Verify;
import org.jetbrains.annotations.NotNull;

public class LabyModConfiguration {

  protected final boolean enabled;
  protected final LabyModServiceDisplay discordRPC;
  protected final LabyModServiceDisplay gameModeSwitchMessages;
  protected final LabyModDiscordRPC discordJoinMatch;
  protected final LabyModDiscordRPC discordSpectateMatch;
  protected final String loginDomain;
  protected final LabyModBanner banner;
  protected final LabyModPermissions permissions;

  public LabyModConfiguration(
    boolean enabled,
    @NotNull LabyModServiceDisplay discordRPC,
    @NotNull LabyModServiceDisplay gameModeSwitchMessages,
    @NotNull LabyModDiscordRPC discordJoinMatch,
    @NotNull LabyModDiscordRPC discordSpectateMatch,
    @NotNull String loginDomain,
    @NotNull LabyModBanner banner,
    @NotNull LabyModPermissions permissions
  ) {
    this.enabled = enabled;
    this.discordRPC = discordRPC;
    this.gameModeSwitchMessages = gameModeSwitchMessages;
    this.discordJoinMatch = discordJoinMatch;
    this.discordSpectateMatch = discordSpectateMatch;
    this.loginDomain = loginDomain;
    this.banner = banner;
    this.permissions = permissions;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull LabyModConfiguration configuration) {
    return builder()
      .enabled(configuration.isEnabled())
      .discordRPC(configuration.getDiscordRPC())
      .gameModeSwitch(configuration.getGameModeSwitchMessages())
      .joinMatch(configuration.getDiscordJoinMatch())
      .spectateMatch(configuration.getDiscordSpectateMatch())
      .loginDomain(configuration.getLoginDomain())
      .banner(configuration.getBanner())
      .permissions(configuration.getPermissions());
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public @NotNull LabyModServiceDisplay getDiscordRPC() {
    return this.discordRPC;
  }

  public @NotNull LabyModServiceDisplay getGameModeSwitchMessages() {
    return this.gameModeSwitchMessages;
  }

  public @NotNull LabyModDiscordRPC getDiscordJoinMatch() {
    return this.discordJoinMatch;
  }

  public @NotNull LabyModDiscordRPC getDiscordSpectateMatch() {
    return this.discordSpectateMatch;
  }

  public @NotNull String getLoginDomain() {
    return this.loginDomain;
  }

  public @NotNull LabyModBanner getBanner() {
    return this.banner;
  }

  public @NotNull LabyModPermissions getPermissions() {
    return this.permissions;
  }

  public static class Builder {

    private boolean enabled = true;
    private LabyModServiceDisplay discordRPC = new LabyModServiceDisplay(true, "Playing on %name%");
    private LabyModServiceDisplay gameModeSwitch = new LabyModServiceDisplay(true, "§bCloud§fNet §8➢ §e%name%");
    private LabyModDiscordRPC discordJoinMatch = LabyModDiscordRPC.builder().build();
    private LabyModDiscordRPC discordSpectateMatch = LabyModDiscordRPC.builder().build();
    private String loginDomain = "mc.cloudnetservice.eu";
    private LabyModBanner banner = new LabyModBanner(false,
      "http://dl.cloudnetservice.eu/data/minecraft/CloudNet-LabyMod-Banner.png");
    private LabyModPermissions permissions = LabyModPermissions.builder().build();

    public @NotNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NotNull Builder discordRPC(@NotNull LabyModServiceDisplay serviceDisplay) {
      this.discordRPC = serviceDisplay;
      return this;
    }

    public @NotNull Builder gameModeSwitch(@NotNull LabyModServiceDisplay serviceDisplay) {
      this.gameModeSwitch = serviceDisplay;
      return this;
    }

    public @NotNull Builder joinMatch(@NotNull LabyModDiscordRPC joinMatch) {
      this.discordJoinMatch = joinMatch;
      return this;
    }

    public @NotNull Builder spectateMatch(@NotNull LabyModDiscordRPC spectateMatch) {
      this.discordSpectateMatch = spectateMatch;
      return this;
    }

    public @NotNull Builder loginDomain(@NotNull String domain) {
      this.loginDomain = domain;
      return this;
    }

    public @NotNull Builder banner(@NotNull LabyModBanner banner) {
      this.banner = banner;
      return this;
    }

    public @NotNull Builder permissions(@NotNull LabyModPermissions permissions) {
      this.permissions = permissions;
      return this;
    }

    public @NotNull LabyModConfiguration build() {
      Verify.verifyNotNull(this.discordRPC, "Missing discord rpc");
      Verify.verifyNotNull(this.gameModeSwitch, "Missing gamemode switch");
      Verify.verifyNotNull(this.discordJoinMatch, "Missing discord join match");
      Verify.verifyNotNull(this.discordSpectateMatch, "Missing discord spectate match");
      Verify.verifyNotNull(this.loginDomain, "Missing login domain");
      Verify.verifyNotNull(this.banner, "Missing banner");
      Verify.verifyNotNull(this.permissions, "Missing permissions");

      return new LabyModConfiguration(this.enabled,
        this.discordRPC,
        this.gameModeSwitch,
        this.discordJoinMatch,
        this.discordSpectateMatch,
        this.loginDomain,
        this.banner,
        this.permissions);
    }
  }

}
