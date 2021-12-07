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

public class LabyModEmoteConfiguration {

  private final int[] emoteIds;
  private final int[] onJoinEmoteIds;
  private final int[] onKnockbackEmoteIds;

  private final long minEmoteDelayTicks;
  private final long maxEmoteDelayTicks;

  private final boolean syncEmotesBetweenNPCs;

  protected LabyModEmoteConfiguration(
    int[] emoteIds,
    int[] onJoinEmoteIds,
    int[] onKnockbackEmoteIds,
    long minEmoteDelayTicks,
    long maxEmoteDelayTicks,
    boolean syncEmotesBetweenNPCs
  ) {
    this.emoteIds = emoteIds;
    this.onJoinEmoteIds = onJoinEmoteIds;
    this.onKnockbackEmoteIds = onKnockbackEmoteIds;
    this.minEmoteDelayTicks = minEmoteDelayTicks;
    this.maxEmoteDelayTicks = maxEmoteDelayTicks;
    this.syncEmotesBetweenNPCs = syncEmotesBetweenNPCs;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull LabyModEmoteConfiguration configuration) {
    return builder()
      .emoteIds(configuration.getEmoteIds())
      .onJoinEmoteIds(configuration.getOnJoinEmoteIds())
      .onKnockbackEmoteIds(configuration.getOnKnockbackEmoteIds())
      .minEmoteDelayTicks(configuration.getMinEmoteDelayTicks())
      .maxEmoteDelayTicks(configuration.getMaxEmoteDelayTicks())
      .syncEmotesBetweenNPCs(configuration.isSyncEmotesBetweenNPCs());
  }

  public int[] getEmoteIds() {
    return this.emoteIds;
  }

  public int[] getOnJoinEmoteIds() {
    return this.onJoinEmoteIds;
  }

  public int[] getOnKnockbackEmoteIds() {
    return this.onKnockbackEmoteIds;
  }

  public long getMinEmoteDelayTicks() {
    return this.minEmoteDelayTicks;
  }

  public long getMaxEmoteDelayTicks() {
    return this.maxEmoteDelayTicks;
  }

  public boolean isSyncEmotesBetweenNPCs() {
    return this.syncEmotesBetweenNPCs;
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

    public @NotNull LabyModEmoteConfiguration build() {
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
