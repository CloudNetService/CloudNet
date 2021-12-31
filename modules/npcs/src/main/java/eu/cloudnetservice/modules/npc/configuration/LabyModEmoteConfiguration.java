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

import lombok.NonNull;

public record LabyModEmoteConfiguration(
  int[] emoteIds,
  int[] onJoinEmoteIds,
  int[] onKnockbackEmoteIds,
  long minEmoteDelayTicks,
  long maxEmoteDelayTicks,
  boolean syncEmotesBetweenNPCs
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull LabyModEmoteConfiguration configuration) {
    return builder()
      .emoteIds(configuration.emoteIds())
      .onJoinEmoteIds(configuration.onJoinEmoteIds())
      .onKnockbackEmoteIds(configuration.onKnockbackEmoteIds())
      .minEmoteDelayTicks(configuration.minEmoteDelayTicks())
      .maxEmoteDelayTicks(configuration.maxEmoteDelayTicks())
      .syncEmotesBetweenNPCs(configuration.syncEmotesBetweenNPCs());
  }

  public static final class Builder {

    private int[] emoteIds = new int[]{2, 3, 49};
    private int[] onJoinEmoteIds = new int[]{4, 20};
    private int[] onKnockbackEmoteIds = new int[]{37};

    private long minEmoteDelayTicks = 20 * 20;
    private long maxEmoteDelayTicks = 30 * 20;

    private boolean syncEmotesBetweenNPCs = false;

    public Builder emoteIds(int[] emoteIds) {
      this.emoteIds = emoteIds;
      return this;
    }

    public Builder onJoinEmoteIds(int[] onJoinEmoteIds) {
      this.onJoinEmoteIds = onJoinEmoteIds;
      return this;
    }

    public Builder onKnockbackEmoteIds(int[] onKnockbackEmoteIds) {
      this.onKnockbackEmoteIds = onKnockbackEmoteIds;
      return this;
    }

    public Builder minEmoteDelayTicks(long minEmoteDelayTicks) {
      this.minEmoteDelayTicks = minEmoteDelayTicks;
      return this;
    }

    public Builder maxEmoteDelayTicks(long maxEmoteDelayTicks) {
      this.maxEmoteDelayTicks = maxEmoteDelayTicks;
      return this;
    }

    public Builder syncEmotesBetweenNPCs(boolean syncEmotesBetweenNPCs) {
      this.syncEmotesBetweenNPCs = syncEmotesBetweenNPCs;
      return this;
    }

    public @NonNull LabyModEmoteConfiguration build() {
      return new LabyModEmoteConfiguration(
        this.emoteIds,
        this.onJoinEmoteIds,
        this.onKnockbackEmoteIds,
        this.minEmoteDelayTicks,
        this.maxEmoteDelayTicks,
        this.syncEmotesBetweenNPCs);
    }
  }
}
