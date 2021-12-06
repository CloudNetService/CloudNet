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
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SyncProxyLoginConfiguration {

  @Include
  protected final String targetGroup;

  protected final boolean maintenance;

  protected final int maxPlayers;

  protected final Set<String> whitelist;
  protected final List<SyncProxyMotd> motds;
  protected final List<SyncProxyMotd> maintenanceMotds;

  protected SyncProxyLoginConfiguration(
    @NotNull String targetGroup,
    boolean maintenance,
    int maxPlayers,
    @NotNull Set<String> whitelist,
    @NotNull List<SyncProxyMotd> motds,
    @NotNull List<SyncProxyMotd> maintenanceMotds
  ) {
    this.targetGroup = targetGroup;
    this.maintenance = maintenance;
    this.maxPlayers = maxPlayers;
    this.whitelist = whitelist;
    this.motds = motds;
    this.maintenanceMotds = maintenanceMotds;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull SyncProxyLoginConfiguration configuration) {
    return builder()
      .targetGroup(configuration.getTargetGroup())
      .maxPlayers(configuration.getMaxPlayers())
      .maintenance(configuration.isMaintenance())
      .whitelist(configuration.getWhitelist())
      .motds(configuration.getMotds())
      .maintenanceMotds(configuration.getMaintenanceMotds());
  }

  public static @NotNull SyncProxyLoginConfiguration createDefault(@NotNull String targetGroup) {
    return builder()
      .targetGroup(targetGroup)
      .maxPlayers(100)
      .addMotd(SyncProxyMotd.builder()
        .firstLine("         &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov3.5 &8┃ &b&o■")
        .secondLine("              &7&onext &3&l&ogeneration &7&onetwork")
        .autoSlotDistance(1)
        .build())
      .addMaintenanceMotd(SyncProxyMotd.builder()
        .firstLine("         &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov3.5 &8┃ &b&o■")
        .secondLine("     &3&lMaintenance &8&l» &7We are still in &3&lmaintenance")
        .autoSlotDistance(1)
        .playerInfo(new String[]{
          " ",
          "&b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov3.5 &8┃ &b&o■",
          "&7Discord &8&l» &bdiscord.cloudnetservice.eu",
          " "
        })
        .protocolText("&8➜ &c&lMaintenance &8┃ &c✘")
        .build())
      .build();
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public boolean isMaintenance() {
    return this.maintenance;
  }

  public int getMaxPlayers() {
    return this.maxPlayers;
  }

  public @NotNull Set<String> getWhitelist() {
    return this.whitelist;
  }

  public @NotNull List<SyncProxyMotd> getMotds() {
    return this.motds;
  }

  public @NotNull List<SyncProxyMotd> getMaintenanceMotds() {
    return this.maintenanceMotds;
  }

  public static class Builder {

    private String targetGroup;
    private boolean maintenance;
    private int maxPlayers;
    private Set<String> whitelist = new HashSet<>();
    private List<SyncProxyMotd> motds = new ArrayList<>();
    private List<SyncProxyMotd> maintenanceMotds = new ArrayList<>();

    public @NotNull Builder targetGroup(@NotNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NotNull Builder maintenance(boolean maintenance) {
      this.maintenance = maintenance;
      return this;
    }

    public @NotNull Builder maxPlayers(int maxPlayers) {
      this.maxPlayers = maxPlayers;
      return this;
    }

    public @NotNull Builder whitelist(@NotNull Set<String> whitelist) {
      this.whitelist = new HashSet<>(whitelist);
      return this;
    }

    public @NotNull Builder addWhitelist(@NotNull String user) {
      this.whitelist.add(user);
      return this;
    }

    public @NotNull Builder motds(@NotNull List<SyncProxyMotd> motds) {
      this.motds = new ArrayList<>(motds);
      return this;
    }

    public @NotNull Builder addMotd(@NotNull SyncProxyMotd motd) {
      this.motds.add(motd);
      return this;
    }

    public @NotNull Builder maintenanceMotds(@NotNull List<SyncProxyMotd> maintenanceMotds) {
      this.maintenanceMotds = maintenanceMotds;
      return this;
    }

    public @NotNull Builder addMaintenanceMotd(@NotNull SyncProxyMotd motd) {
      this.maintenanceMotds.add(motd);
      return this;
    }

    public @NotNull SyncProxyLoginConfiguration build() {
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
