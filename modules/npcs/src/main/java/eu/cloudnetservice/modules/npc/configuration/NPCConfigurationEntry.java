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

package eu.cloudnetservice.modules.npc.configuration;

import org.jetbrains.annotations.NotNull;

public class NPCConfigurationEntry {

  private final String targetGroup;

  private final double infoLineDistance;

  private final double knockbackDistance;
  private final double knockbackStrength;

  private final NPCPoolOptions npcPoolOptions;
  private final LabyModEmoteConfiguration emoteConfiguration;

  private final InventoryConfiguration inventoryConfiguration;

  protected NPCConfigurationEntry(
    @NotNull String targetGroup,
    double infoLineDistance,
    double knockbackDistance,
    double knockbackStrength,
    @NotNull NPCPoolOptions npcPoolOptions,
    @NotNull LabyModEmoteConfiguration emoteConfiguration,
    @NotNull InventoryConfiguration inventoryConfiguration
  ) {
    this.targetGroup = targetGroup;
    this.infoLineDistance = infoLineDistance;
    this.knockbackDistance = knockbackDistance;
    this.knockbackStrength = knockbackStrength;
    this.npcPoolOptions = npcPoolOptions;
    this.emoteConfiguration = emoteConfiguration;
    this.inventoryConfiguration = inventoryConfiguration;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull NPCConfigurationEntry entry) {
    return builder()
      .targetGroup(entry.getTargetGroup())
      .infoLineDistance(entry.getInfoLineDistance())
      .knockbackDistance(entry.getKnockbackDistance())
      .knockbackStrength(entry.getKnockbackStrength())
      .npcPoolOptions(entry.getNpcPoolOptions())
      .emoteConfiguration(entry.getEmoteConfiguration())
      .inventoryConfiguration(entry.getInventoryConfiguration());
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public double getInfoLineDistance() {
    return this.infoLineDistance;
  }

  public double getKnockbackDistance() {
    return this.knockbackDistance;
  }

  public double getKnockbackStrength() {
    return this.knockbackStrength;
  }

  public @NotNull NPCPoolOptions getNpcPoolOptions() {
    return this.npcPoolOptions;
  }

  public @NotNull LabyModEmoteConfiguration getEmoteConfiguration() {
    return this.emoteConfiguration;
  }

  public @NotNull InventoryConfiguration getInventoryConfiguration() {
    return this.inventoryConfiguration;
  }

  public static class Builder {

    private String targetGroup;

    private double infoLineDistance = 0.25D;

    private double knockbackDistance = 0.7D;
    private double knockbackStrength = 0.8D;

    private LabyModEmoteConfiguration emoteConfiguration = LabyModEmoteConfiguration.builder().build();
    private NPCPoolOptions npcPoolOptions = NPCPoolOptions.builder().build();

    private InventoryConfiguration inventoryConfiguration = InventoryConfiguration.builder().build();

    public @NotNull Builder targetGroup(@NotNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NotNull Builder infoLineDistance(double infoLineDistance) {
      this.infoLineDistance = infoLineDistance;
      return this;
    }

    public @NotNull Builder knockbackDistance(double knockbackDistance) {
      this.knockbackDistance = knockbackDistance;
      return this;
    }

    public @NotNull Builder knockbackStrength(double knockbackStrength) {
      this.knockbackStrength = knockbackStrength;
      return this;
    }

    public @NotNull Builder npcPoolOptions(@NotNull NPCPoolOptions npcPoolOptions) {
      this.npcPoolOptions = npcPoolOptions;
      return this;
    }

    public @NotNull Builder emoteConfiguration(@NotNull LabyModEmoteConfiguration emoteConfiguration) {
      this.emoteConfiguration = emoteConfiguration;
      return this;
    }

    public @NotNull Builder inventoryConfiguration(@NotNull InventoryConfiguration inventoryConfiguration) {
      this.inventoryConfiguration = inventoryConfiguration;
      return this;
    }

    public @NotNull NPCConfigurationEntry build() {
      return new NPCConfigurationEntry(
        this.targetGroup,
        this.infoLineDistance,
        this.knockbackDistance,
        this.knockbackStrength,
        this.npcPoolOptions,
        this.emoteConfiguration,
        this.inventoryConfiguration);
    }
  }
}
