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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Unmodifiable;

public record SyncProxyLoginConfiguration(
  @NonNull String targetGroup,
  boolean maintenance,
  int maxPlayers,
  @Unmodifiable  @NonNull Set<String> whitelist,
  @Unmodifiable @NonNull List<SyncProxyMotd> motds,
  @Unmodifiable @NonNull List<SyncProxyMotd> maintenanceMotds
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SyncProxyLoginConfiguration configuration) {
    return builder()
      .targetGroup(configuration.targetGroup())
      .maxPlayers(configuration.maxPlayers())
      .maintenance(configuration.maintenance())
      .whitelist(configuration.whitelist())
      .motds(configuration.motds())
      .maintenanceMotds(configuration.maintenanceMotds());
  }

  public static @NonNull SyncProxyLoginConfiguration createDefault(@NonNull String targetGroup) {
    return builder()
      .targetGroup(targetGroup)
      .maxPlayers(100)
      .modifyMotd(motds -> motds.add(SyncProxyMotd.builder()
          .firstLine(Component.text("         ", NamedTextColor.DARK_GRAY)
            .append(Component.text("■ ", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
            .append(Component.text("┃ "))
            .append(Component.text("CloudNet ", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
            .append(Component.text("● "))
            .append(Component.text("Blizzard ", NamedTextColor.RED))
            .append(Component.text("┃ "))
            .append(Component.text("■", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
          )
          .secondLine(Component.text("              ", NamedTextColor.GRAY)
            .append(Component.text("next ").decorate(TextDecoration.ITALIC))
            .append(Component.text("generation ", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC))
            .append(Component.text("network").decorate(TextDecoration.ITALIC))
          )
        .autoSlotDistance(1)
        .build()))
      .modifyMaintenanceMotd(motds -> motds.add(SyncProxyMotd.builder()
          .firstLine(Component.text("         ", NamedTextColor.DARK_GRAY)
            .append(Component.text("■ ", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
            .append(Component.text("┃ "))
            .append(Component.text("CloudNet ", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
            .append(Component.text("● "))
            .append(Component.text("Blizzard ", NamedTextColor.RED))
            .append(Component.text("┃ "))
            .append(Component.text("■", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
          )
          .secondLine(Component.text("     ")
            .append(Component.text("» ", NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD))
            .append(Component.text("We are still in ", NamedTextColor.GRAY))
            .append(Component.text("maintenance", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
          )
          .autoSlotDistance(1)
          .playerInfo(List.of(
            Component.space(),
            Component.empty()
              .append(Component.text("■ ", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
              .append(Component.text("┃ ", NamedTextColor.DARK_GRAY))
              .append(Component.text("CloudNet ", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
              .append(Component.text("● ", NamedTextColor.DARK_GRAY))
              .append(Component.text("Blizzard ", NamedTextColor.RED))
              .append(Component.text("┃ ", NamedTextColor.DARK_GRAY))
              .append(Component.text("■", NamedTextColor.AQUA).decorate(TextDecoration.ITALIC)),
            Component.text("Discord ", NamedTextColor.GRAY)
              .append(Component.text("» ", NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD))
              .append(Component.text("discord.cloudnetservice.eu", NamedTextColor.AQUA)),
            Component.space()
          ))
          .protocolText(Component.text("➜ ", NamedTextColor.DARK_GRAY)
            .append(Component.text("Maintenance ", NamedTextColor.RED).decorate(TextDecoration.BOLD))
            .append(Component.text("┃ ", NamedTextColor.DARK_GRAY))
            .append(Component.text("✘", NamedTextColor.RED))
          )
          .build()
      ))
      .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SyncProxyLoginConfiguration that)) {
      return false;
    }

    return this.targetGroup.equals(that.targetGroup);
  }

  @Override
  public int hashCode() {
    return this.targetGroup.hashCode();
  }

  public static class Builder {

    private String targetGroup;
    private boolean maintenance;
    private int maxPlayers;
    private Set<String> whitelist = new HashSet<>();
    private List<SyncProxyMotd> motds = new ArrayList<>();
    private List<SyncProxyMotd> maintenanceMotds = new ArrayList<>();

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder maintenance(boolean maintenance) {
      this.maintenance = maintenance;
      return this;
    }

    public @NonNull Builder maxPlayers(int maxPlayers) {
      this.maxPlayers = maxPlayers;
      return this;
    }

    public @NonNull Builder whitelist(@NonNull Set<String> whitelist) {
      this.whitelist = new HashSet<>(whitelist);
      return this;
    }

    public @NonNull Builder modifyWhitelist(@NonNull Consumer<Set<String>> modifier) {
      modifier.accept(this.whitelist);
      return this;
    }

    public @NonNull Builder motds(@NonNull List<SyncProxyMotd> motds) {
      this.motds = new ArrayList<>(motds);
      return this;
    }

    public @NonNull Builder modifyMotd(@NonNull Consumer<List<SyncProxyMotd>> modifier) {
      modifier.accept(this.motds);
      return this;
    }

    public @NonNull Builder maintenanceMotds(@NonNull List<SyncProxyMotd> maintenanceMotds) {
      this.maintenanceMotds = maintenanceMotds;
      return this;
    }

    public @NonNull Builder modifyMaintenanceMotd(@NonNull Consumer<List<SyncProxyMotd>> modifier) {
      modifier.accept(this.maintenanceMotds);
      return this;
    }

    public @NonNull SyncProxyLoginConfiguration build() {
      Preconditions.checkNotNull(this.targetGroup, "Missing targetGroup");

      return new SyncProxyLoginConfiguration(
        this.targetGroup,
        this.maintenance,
        this.maxPlayers,
        Set.copyOf(this.whitelist),
        List.copyOf(this.motds),
        List.copyOf(this.maintenanceMotds));
    }
  }
}
