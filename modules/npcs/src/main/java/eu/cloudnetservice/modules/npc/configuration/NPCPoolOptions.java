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

import lombok.NonNull;

public record NPCPoolOptions(int spawnDistance, int actionDistance, long tabListRemoveTicks) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull NPCPoolOptions options) {
    return builder()
      .spawnDistance(options.spawnDistance())
      .actionDistance(options.actionDistance())
      .tabListRemoveTicks(options.tabListRemoveTicks());
  }

  public static final class Builder {

    private int spawnDistance = 50;
    private int actionDistance = 20;
    private long tabListRemoveTicks = 30;

    public @NonNull Builder spawnDistance(int spawnDistance) {
      this.spawnDistance = spawnDistance;
      return this;
    }

    public @NonNull Builder actionDistance(int actionDistance) {
      this.actionDistance = actionDistance;
      return this;
    }

    public @NonNull Builder tabListRemoveTicks(long tabListRemoveTicks) {
      this.tabListRemoveTicks = tabListRemoveTicks;
      return this;
    }

    public @NonNull NPCPoolOptions build() {
      return new NPCPoolOptions(this.spawnDistance, this.actionDistance, this.tabListRemoveTicks);
    }
  }
}
