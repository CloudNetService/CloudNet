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
        .firstLine("         "
          + "<aqua><italic>■</italic></aqua> "
          + "<dark_gray>┃</dark_gray> "
          + "<dark_aqua><bold>CloudNet</bold></dark_aqua> "
          + "<dark_gray>●</dark_gray> "
          + "<red>Blizzard</red> "
          + "<dark_gray>┃</dark_gray> "
          + "<aqua><italic>■</italic></aqua>")
        .secondLine("              "
          + "<gray><italic>next</italic></gray> "
          + "<dark_aqua><bold><italic>generation</italic></bold></dark_aqua> "
          + "<gray><italic>network</italic></gray>")
        .autoSlotDistance(1)
        .build()))
      .modifyMaintenanceMotd(motds -> motds.add(SyncProxyMotd.builder()
        .firstLine("         "
          + "<aqua><italic>■</italic></aqua> "
          + "<dark_gray>┃</dark_gray> "
          + "<dark_aqua><bold>CloudNet</bold></dark_aqua> "
          + "<dark_gray>●</dark_gray> "
          + "<red>Blizzard</red> "
          + "<dark_gray>┃</dark_gray> "
          + "<aqua><italic>■</italic></aqua>")
        .secondLine("     "
          + "<dark_aqua><bold>Maintenance</bold></dark_aqua> "
          + "<dark_gray><bold>»</bold></dark_gray> "
          + "<gray>We are still in</gray> "
          + "<dark_aqua><bold>maintenance</bold></dark_aqua>")
        .autoSlotDistance(1)
        .playerInfo(new String[]{
          " ",
          "<aqua><italic>■</italic></aqua> "
            + "<dark_gray>┃</dark_gray> "
            + "<dark_aqua><bold>CloudNet</bold></dark_aqua> "
            + "<dark_gray>●</dark_gray> "
            + "<red>Blizzard</red> "
            + "<dark_gray>┃</dark_gray> "
            + "<aqua><italic>■</italic></aqua>",
          "<gray>Discord</gray> "
            + "<dark_gray><bold>»</bold></dark_gray> "
            + "<aqua>discord.cloudnetservice.eu</aqua>",
          " "
        })
        .protocolText("<dark_gray>➜</dark_gray> "
          + "<red><bold>Maintenance</bold></red> "
          + "<dark_gray>┃</dark_gray> <red>✘</red>")
        .build()))
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
