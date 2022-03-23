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

package eu.cloudnetservice.modules.signs.configuration;

import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record SignConfigurationEntry(
  @NonNull String targetGroup,
  boolean switchToSearchingWhenServiceIsFull,
  @NonNull KnockbackConfiguration knockbackConfiguration,
  @NonNull List<SignGroupConfiguration> groupConfigurations,
  @NonNull SignLayoutsHolder searchingLayout,
  @NonNull SignLayoutsHolder startingLayout,
  @NonNull SignLayoutsHolder emptyLayout,
  @NonNull SignLayoutsHolder onlineLayout,
  @NonNull SignLayoutsHolder fullLayout
) {

  public static @NonNull SignConfigurationEntry createDefault(
    @NonNull String targetGroup,
    @NonNull String onlineBlockType,
    @NonNull String fullBlockType,
    @NonNull String startingBlock,
    @NonNull String searchingBlock
  ) {
    return SignConfigurationEntry.builder()
      .targetGroup(targetGroup)
      .groupConfigurations(List.of(SignGroupConfiguration.builder()
        .targetGroup("TARGET_GROUP")
        .emptyLayout(SignLayoutsHolder.singleLayout(SignLayout.builder()
          .lines("&7Lobby &0- &7%task_id%", "&8[&7LOBBY&8]", "%online_players% / %max_players%", "%motd%")
          .blockMaterial(onlineBlockType)
          .build()))
        .onlineLayout(SignLayoutsHolder.singleLayout(SignLayout.builder()
          .lines("&eLobby &0- &e%task_id%", "&8[&eLOBBY&8]", "%online_players% / %max_players%", "%motd%")
          .blockMaterial(onlineBlockType)
          .build()))
        .fullLayout(SignLayoutsHolder.singleLayout(SignLayout.builder()
          .lines("&6Lobby &0- &6%task_id%", "&8[&6PRIME&8]", "%online_players% / %max_players%", "%motd%")
          .blockMaterial(onlineBlockType)
          .build()))
        .build()))
      .searchingLayout(defaultLayout("Waiting", searchingBlock))
      .startingLayout(defaultLayout("Starting", startingBlock))
      .emptyLayout(defaultLayout("&8[&7LOBBY&8]", onlineBlockType))
      .onlineLayout(defaultLayout("&8[&eLOBBY&8]", onlineBlockType))
      .fullLayout(defaultLayout("&8[&6&lLOBBY&8]", fullBlockType))
      .build();
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SignConfigurationEntry entry) {
    return builder()
      .targetGroup(entry.targetGroup())
      .switchToSearchingWhenServiceIsFull(entry.switchToSearchingWhenServiceIsFull())
      .knockbackConfiguration(entry.knockbackConfiguration())
      .groupConfigurations(entry.groupConfigurations())
      .searchingLayout(entry.searchingLayout())
      .startingLayout(entry.startingLayout())
      .emptyLayout(entry.emptyLayout())
      .onlineLayout(entry.onlineLayout())
      .fullLayout(entry.fullLayout());
  }

  private static @NonNull SignLayoutsHolder defaultLayout(
    @NonNull String firstLine,
    @NonNull String block
  ) {
    List<SignLayout> signLayouts = new ArrayList<>(3);
    for (int i = 1; i <= 3; i++) {
      signLayouts.add(SignLayout.builder()
        .lines("", firstLine, ".".repeat(i), "")
        .blockMaterial(block)
        .build());
    }
    return new SignLayoutsHolder(1, signLayouts);
  }

  public static class Builder {

    private String targetGroup;
    private boolean switchToSearchingWhenServiceIsFull;
    private KnockbackConfiguration knockbackConfiguration = KnockbackConfiguration.builder().build();
    private List<SignGroupConfiguration> groupConfigurations = new ArrayList<>();
    private SignLayoutsHolder searchingLayout;
    private SignLayoutsHolder startingLayout;
    private SignLayoutsHolder emptyLayout;
    private SignLayoutsHolder onlineLayout;
    private SignLayoutsHolder fullLayout;

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder switchToSearchingWhenServiceIsFull(boolean switchToSearchingWhenServiceIsFull) {
      this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
      return this;
    }

    public @NonNull Builder knockbackConfiguration(@NonNull KnockbackConfiguration knockbackConfiguration) {
      this.knockbackConfiguration = knockbackConfiguration;
      return this;
    }

    public @NonNull Builder groupConfigurations(@NonNull List<SignGroupConfiguration> groupConfigurations) {
      this.groupConfigurations = new ArrayList<>(groupConfigurations);
      return this;
    }

    public @NonNull Builder searchingLayout(@NonNull SignLayoutsHolder searchingLayout) {
      this.searchingLayout = searchingLayout;
      return this;
    }

    public @NonNull Builder startingLayout(@NonNull SignLayoutsHolder startingLayout) {
      this.startingLayout = startingLayout;
      return this;
    }

    public @NonNull Builder emptyLayout(@NonNull SignLayoutsHolder emptyLayout) {
      this.emptyLayout = emptyLayout;
      return this;
    }

    public @NonNull Builder onlineLayout(@NonNull SignLayoutsHolder onlineLayout) {
      this.onlineLayout = onlineLayout;
      return this;
    }

    public @NonNull Builder fullLayout(@NonNull SignLayoutsHolder fullLayout) {
      this.fullLayout = fullLayout;
      return this;
    }

    public @NonNull SignConfigurationEntry build() {
      Verify.verifyNotNull(this.targetGroup, "Missing target group");
      Verify.verifyNotNull(this.searchingLayout, "Missing searching layout");
      Verify.verifyNotNull(this.startingLayout, "Missing starting layout");
      Verify.verifyNotNull(this.emptyLayout, "Missing empty layout");
      Verify.verifyNotNull(this.onlineLayout, "Missing online layout");
      Verify.verifyNotNull(this.fullLayout, "Missing full layout");

      return new SignConfigurationEntry(
        this.targetGroup,
        this.switchToSearchingWhenServiceIsFull,
        this.knockbackConfiguration,
        this.groupConfigurations,
        this.searchingLayout,
        this.startingLayout,
        this.emptyLayout,
        this.onlineLayout,
        this.fullLayout);
    }
  }

  public record KnockbackConfiguration(
    boolean enabled,
    double distance,
    double strength,
    @Nullable String bypassPermission
  ) {

    public static Builder builder() {
      return new Builder();
    }

    public static Builder builder(@NonNull KnockbackConfiguration configuration) {
      return builder()
        .enabled(configuration.enabled())
        .distance(configuration.distance())
        .strength(configuration.strength())
        .bypassPermission(configuration.bypassPermission());
    }

    public boolean validAndEnabled() {
      return this.enabled && this.strength > 0 && this.distance > 0;
    }

    public static class Builder {

      private boolean enabled = true;
      private double distance = 1;
      private double strength = 0.8;
      private String bypassPermission = "cloudnet.signs.knockback.bypass";

      public @NonNull Builder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      public @NonNull Builder distance(double distance) {
        this.distance = distance;
        return this;
      }

      public @NonNull Builder strength(double strength) {
        this.strength = strength;
        return this;
      }

      public @NonNull Builder bypassPermission(@Nullable String bypassPermission) {
        this.bypassPermission = bypassPermission;
        return this;
      }

      public @NonNull KnockbackConfiguration build() {
        return new KnockbackConfiguration(this.enabled, this.distance, this.strength, this.bypassPermission);
      }
    }
  }
}
