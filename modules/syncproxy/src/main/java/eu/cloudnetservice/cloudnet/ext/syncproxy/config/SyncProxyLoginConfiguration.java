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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;

public record SyncProxyLoginConfiguration(
  @NonNull String targetGroup,
  boolean maintenance,
  int maxPlayers,
  @NonNull Set<String> whitelist,
  @NonNull List<SyncProxyMotd> motds,
  @NonNull List<SyncProxyMotd> maintenanceMotds
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
      .addMotd(SyncProxyMotd.builder()
        .firstLine("         &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov4.0 &8┃ &b&o■")
        .secondLine("              &7&onext &3&l&ogeneration &7&onetwork")
        .autoSlotDistance(1)
        .build())
      .addMaintenanceMotd(SyncProxyMotd.builder()
        .firstLine("         &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov4.0 &8┃ &b&o■")
        .secondLine("     &3&lMaintenance &8&l» &7We are still in &3&lmaintenance")
        .autoSlotDistance(1)
        .playerInfo(new String[]{
          " ",
          "&b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov4.0 &8┃ &b&o■",
          "&7Discord &8&l» &bdiscord.cloudnetservice.eu",
          " "
        })
        .protocolText("&8➜ &c&lMaintenance &8┃ &c✘")
        .build())
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

    public @NonNull Builder addWhitelist(@NonNull String user) {
      this.whitelist.add(user);
      return this;
    }

    public @NonNull Builder motds(@NonNull List<SyncProxyMotd> motds) {
      this.motds = new ArrayList<>(motds);
      return this;
    }

    public @NonNull Builder addMotd(@NonNull SyncProxyMotd motd) {
      this.motds.add(motd);
      return this;
    }

    public @NonNull Builder maintenanceMotds(@NonNull List<SyncProxyMotd> maintenanceMotds) {
      this.maintenanceMotds = maintenanceMotds;
      return this;
    }

    public @NonNull Builder addMaintenanceMotd(@NonNull SyncProxyMotd motd) {
      this.maintenanceMotds.add(motd);
      return this;
    }

    public @NonNull SyncProxyLoginConfiguration build() {
      Verify.verifyNotNull(this.targetGroup, "Missing targetGroup");

      return new SyncProxyLoginConfiguration(this.targetGroup,
        this.maintenance,
        this.maxPlayers,
        this.whitelist,
        this.motds,
        this.maintenanceMotds);
    }
  }
}
