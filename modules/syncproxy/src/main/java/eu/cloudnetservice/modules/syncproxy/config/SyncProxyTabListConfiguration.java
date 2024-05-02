/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.syncproxy.config;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.ext.component.InternalPlaceholder;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SyncProxyTabListConfiguration {

  @EqualsAndHashCode.Include
  protected final String targetGroup;

  protected final List<SyncProxyTabList> entries;
  protected final double animationsPerSecond;

  protected transient int currentEntry = -1;

  protected SyncProxyTabListConfiguration(
    @NonNull String targetGroup,
    @NonNull List<SyncProxyTabList> entries,
    double animationsPerSecond
  ) {
    this.targetGroup = targetGroup;
    this.entries = entries;
    this.animationsPerSecond = animationsPerSecond;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SyncProxyTabListConfiguration configuration) {
    return builder()
      .targetGroup(configuration.targetGroup())
      .tabListEntries(configuration.entries())
      .animationsPerSecond(configuration.animationsPerSecond());
  }

  public static @NonNull SyncProxyTabListConfiguration createDefault(String targetGroup) {
    return builder()
      .targetGroup(targetGroup)
      .addTabListEntry(SyncProxyTabList.builder()
        .header(Component.space()
          .appendNewline()
          .append(Component.text(" ■ ", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
          .append(Component.text("┃ ", NamedTextColor.DARK_GRAY))
          .append(Component.text("CloudNet ", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
          .append(Component.text("● ", NamedTextColor.DARK_GRAY))
          .append(Component.text("Blizzard ", NamedTextColor.RED))
          .append(Component.text("» ", NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD))
          .append(InternalPlaceholder.create("online_players").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
          .append(Component.text("/", NamedTextColor.DARK_GRAY))
          .append(InternalPlaceholder.create("max_players").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
          .append(Component.text(" ┃ ", NamedTextColor.DARK_GRAY))
          .append(Component.text("■", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
          .appendNewline()
          .append(Component.text("► ", NamedTextColor.DARK_GRAY))
          .append(Component.text("Current server ", NamedTextColor.GRAY))
          .append(Component.text("● ", NamedTextColor.DARK_GRAY))
          .append(InternalPlaceholder.create("server").color(NamedTextColor.AQUA))
          .append(Component.text(" ◄ ", NamedTextColor.DARK_GRAY))
          .appendNewline()
          .appendSpace()
        )
        .footer(Component.space()
          .appendNewline()
          .append(Component.text(" Discord ", NamedTextColor.GRAY))
          .append(Component.text("» ", NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD))
          .append(Component.text("discord.cloudnetservice.eu", NamedTextColor.AQUA))
          .appendNewline()
          .append(Component.text(" next ", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
          .append(Component.text("generation ", NamedTextColor.DARK_AQUA)).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC)
          .append(Component.text("network ", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
          .appendNewline()
          .appendSpace()
        )
        .build())
      .animationsPerSecond(1.0)
      .build();
  }

  public @NonNull String targetGroup() {
    return this.targetGroup;
  }

  public @NonNull List<SyncProxyTabList> entries() {
    return this.entries;
  }

  public double animationsPerSecond() {
    return this.animationsPerSecond;
  }

  public @NonNull SyncProxyTabList tick() {
    if (++this.currentEntry >= this.entries.size()) {
      this.currentEntry = 0;
    }

    return this.currentEntry();
  }

  public @NonNull SyncProxyTabList currentEntry() {
    return this.entries().get(this.currentTick());
  }

  public int currentTick() {
    return this.currentEntry;
  }

  public static class Builder {

    private String targetGroup;
    private List<SyncProxyTabList> entries = new ArrayList<>();
    private double animationsPerSecond;

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder tabListEntries(@NonNull List<SyncProxyTabList> entries) {
      this.entries = new ArrayList<>(entries);
      return this;
    }

    public @NonNull Builder addTabListEntry(@NonNull SyncProxyTabList tabList) {
      this.entries.add(tabList);
      return this;
    }

    public @NonNull Builder animationsPerSecond(double animationsPerSecond) {
      this.animationsPerSecond = animationsPerSecond;
      return this;
    }

    public @NonNull SyncProxyTabListConfiguration build() {
      Preconditions.checkNotNull(this.targetGroup, "Missing targetGroup");

      return new SyncProxyTabListConfiguration(this.targetGroup, this.entries, this.animationsPerSecond);
    }
  }
}
