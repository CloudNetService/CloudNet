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

package eu.cloudnetservice.modules.npc.configuration;

import com.google.common.base.Preconditions;
import lombok.NonNull;

public record NPCConfigurationEntry(
  @NonNull String targetGroup,
  double infoLineDistance,
  double knockbackDistance,
  double knockbackStrength,
  @NonNull NPCPoolOptions npcPoolOptions,
  @NonNull LabyModEmoteConfiguration emoteConfiguration,
  @NonNull InventoryConfiguration inventoryConfiguration
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull NPCConfigurationEntry entry) {
    return builder()
      .targetGroup(entry.targetGroup())
      .infoLineDistance(entry.infoLineDistance())
      .knockbackDistance(entry.knockbackDistance())
      .knockbackStrength(entry.knockbackStrength())
      .npcPoolOptions(entry.npcPoolOptions())
      .emoteConfiguration(entry.emoteConfiguration())
      .inventoryConfiguration(entry.inventoryConfiguration());
  }

  public static class Builder {

    private String targetGroup;

    private double infoLineDistance = 0.25D;

    private double knockbackDistance = 0.7D;
    private double knockbackStrength = 0.8D;

    private LabyModEmoteConfiguration emoteConfiguration = LabyModEmoteConfiguration.builder().build();
    private NPCPoolOptions npcPoolOptions = NPCPoolOptions.builder().build();

    private InventoryConfiguration inventoryConfiguration = InventoryConfiguration.builder().build();

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder infoLineDistance(double infoLineDistance) {
      this.infoLineDistance = infoLineDistance;
      return this;
    }

    public @NonNull Builder knockbackDistance(double knockbackDistance) {
      this.knockbackDistance = knockbackDistance;
      return this;
    }

    public @NonNull Builder knockbackStrength(double knockbackStrength) {
      this.knockbackStrength = knockbackStrength;
      return this;
    }

    public @NonNull Builder npcPoolOptions(@NonNull NPCPoolOptions npcPoolOptions) {
      this.npcPoolOptions = npcPoolOptions;
      return this;
    }

    public @NonNull Builder emoteConfiguration(@NonNull LabyModEmoteConfiguration emoteConfiguration) {
      this.emoteConfiguration = emoteConfiguration;
      return this;
    }

    public @NonNull Builder inventoryConfiguration(@NonNull InventoryConfiguration inventoryConfiguration) {
      this.inventoryConfiguration = inventoryConfiguration;
      return this;
    }

    public @NonNull NPCConfigurationEntry build() {
      Preconditions.checkNotNull(this.targetGroup, "Missing npc entry targetGroup");

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
