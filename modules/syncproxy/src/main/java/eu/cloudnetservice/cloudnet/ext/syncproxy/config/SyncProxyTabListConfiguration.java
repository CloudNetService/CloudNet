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

package eu.cloudnetservice.cloudnet.ext.syncproxy.config;

import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SyncProxyTabListConfiguration {

  @Include
  protected final String targetGroup;

  protected final List<SyncProxyTabList> entries;
  protected final double animationsPerSecond;

  protected final transient AtomicInteger currentEntry;

  protected SyncProxyTabListConfiguration(
    @NotNull String targetGroup,
    @NotNull List<SyncProxyTabList> entries,
    double animationsPerSecond
  ) {
    this.targetGroup = targetGroup;
    this.entries = entries;
    this.animationsPerSecond = animationsPerSecond;

    this.currentEntry = new AtomicInteger(-1);
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull SyncProxyTabListConfiguration configuration) {
    return builder()
      .targetGroup(configuration.getTargetGroup())
      .tabListEntries(configuration.getEntries())
      .animationsPerSecond(configuration.getAnimationsPerSecond());
  }

  public static @NotNull SyncProxyTabListConfiguration createDefault(String targetGroup) {
    return builder()
      .targetGroup(targetGroup)
      .addTabListEntry(SyncProxyTabList.builder()
        .header(" \n &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&o%online_players%&8/&7&o%max_players% &8┃ &b&o■ "
          + "\n &8► &7Current server &8● &b%server% &8◄ \n ")
        .footer(" \n &7Discord &8&l» &bdiscord.cloudnetservice.eu \n &7&onext &3&l&ogeneration &7&onetwork \n ")
        .build())
      .animationsPerSecond(1.0)
      .build();
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public @NotNull List<SyncProxyTabList> getEntries() {
    return this.entries;
  }

  public double getAnimationsPerSecond() {
    return this.animationsPerSecond;
  }

  public @NotNull SyncProxyTabList tick() {
    if (this.currentEntry.incrementAndGet() >= this.entries.size()) {
      this.currentEntry.set(0);
    }

    return this.getCurrentEntry();
  }

  public @NotNull SyncProxyTabList getCurrentEntry() {
    return this.getEntries().get(this.getCurrentTick());
  }

  public int getCurrentTick() {
    return this.currentEntry.get();
  }

  public static class Builder {

    private String targetGroup;
    private List<SyncProxyTabList> entries = new ArrayList<>();
    private double animationsPerSecond;

    public @NotNull Builder targetGroup(@NotNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NotNull Builder tabListEntries(@NotNull List<SyncProxyTabList> entries) {
      this.entries = new ArrayList<>(entries);
      return this;
    }

    public @NotNull Builder addTabListEntry(@NotNull SyncProxyTabList tabList) {
      this.entries.add(tabList);
      return this;
    }

    public @NotNull Builder animationsPerSecond(double animationsPerSecond) {
      this.animationsPerSecond = animationsPerSecond;
      return this;
    }

    public @NotNull SyncProxyTabListConfiguration build() {
      Verify.verifyNotNull(this.targetGroup, "Missing targetGroup");

      return new SyncProxyTabListConfiguration(this.targetGroup, this.entries, this.animationsPerSecond);
    }
  }
}
