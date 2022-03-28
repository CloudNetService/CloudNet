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

package eu.cloudnetservice.modules.labymod.config;

import com.google.common.base.Preconditions;
import lombok.NonNull;

public record LabyModConfiguration(
  boolean enabled,
  @NonNull LabyModServiceDisplay discordRPC,
  @NonNull LabyModServiceDisplay gameModeSwitchMessages,
  @NonNull LabyModDiscordRPC discordJoinMatch,
  @NonNull LabyModDiscordRPC discordSpectateMatch,
  @NonNull String loginDomain,
  @NonNull LabyModBanner banner,
  @NonNull LabyModPermissions permissions
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull LabyModConfiguration configuration) {
    return builder()
      .enabled(configuration.enabled())
      .discordRPC(configuration.discordRPC())
      .gameModeSwitch(configuration.gameModeSwitchMessages())
      .joinMatch(configuration.discordJoinMatch())
      .spectateMatch(configuration.discordSpectateMatch())
      .loginDomain(configuration.loginDomain())
      .banner(configuration.banner())
      .permissions(configuration.permissions());
  }

  @SuppressWarnings("HttpUrlsUsage") // LabyMod 1.8 is too old
  public static class Builder {

    private boolean enabled = true;

    private LabyModServiceDisplay discordRPC = new LabyModServiceDisplay(true, "Playing on %name%");
    private LabyModServiceDisplay gameModeSwitch = new LabyModServiceDisplay(true, "§bCloud§fNet §8➢ §e%name%");

    private LabyModDiscordRPC discordJoinMatch = LabyModDiscordRPC.builder().build();
    private LabyModDiscordRPC discordSpectateMatch = LabyModDiscordRPC.builder().build();

    private String loginDomain = "mc.cloudnetservice.eu";
    private LabyModBanner banner = new LabyModBanner(
      false,
      "http://dl.cloudnetservice.eu/data/minecraft/CloudNet-LabyMod-Banner.png");

    private LabyModPermissions permissions = LabyModPermissions.builder().build();

    public @NonNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NonNull Builder discordRPC(@NonNull LabyModServiceDisplay serviceDisplay) {
      this.discordRPC = serviceDisplay;
      return this;
    }

    public @NonNull Builder gameModeSwitch(@NonNull LabyModServiceDisplay serviceDisplay) {
      this.gameModeSwitch = serviceDisplay;
      return this;
    }

    public @NonNull Builder joinMatch(@NonNull LabyModDiscordRPC joinMatch) {
      this.discordJoinMatch = joinMatch;
      return this;
    }

    public @NonNull Builder spectateMatch(@NonNull LabyModDiscordRPC spectateMatch) {
      this.discordSpectateMatch = spectateMatch;
      return this;
    }

    public @NonNull Builder loginDomain(@NonNull String domain) {
      this.loginDomain = domain;
      return this;
    }

    public @NonNull Builder banner(@NonNull LabyModBanner banner) {
      this.banner = banner;
      return this;
    }

    public @NonNull Builder permissions(@NonNull LabyModPermissions permissions) {
      this.permissions = permissions;
      return this;
    }

    public @NonNull LabyModConfiguration build() {
      Preconditions.checkNotNull(this.discordRPC, "Missing discord rpc");
      Preconditions.checkNotNull(this.gameModeSwitch, "Missing gamemode switch");
      Preconditions.checkNotNull(this.discordJoinMatch, "Missing discord join match");
      Preconditions.checkNotNull(this.discordSpectateMatch, "Missing discord spectate match");
      Preconditions.checkNotNull(this.loginDomain, "Missing login domain");
      Preconditions.checkNotNull(this.banner, "Missing banner");
      Preconditions.checkNotNull(this.permissions, "Missing permissions");

      return new LabyModConfiguration(
        this.enabled,
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
