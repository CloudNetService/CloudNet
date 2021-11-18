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

import java.util.Collections;
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
  protected String targetGroup;

  protected boolean maintenance;

  protected int maxPlayers;

  protected Set<String> whitelist;
  protected List<SyncProxyMotd> motds;
  protected List<SyncProxyMotd> maintenanceMotds;

  public SyncProxyLoginConfiguration(
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

  public static @NotNull SyncProxyLoginConfiguration createDefault(String targetGroup) {
    return new SyncProxyLoginConfiguration(
      targetGroup,
      false,
      100,
      new HashSet<>(),
      Collections.singletonList(new SyncProxyMotd(
        "         &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov3.5 &8┃ &b&o■",
        "              &7&onext &3&l&ogeneration &7&onetwork",
        false,
        1,
        new String[]{},
        null
      )),
      Collections.singletonList(new SyncProxyMotd(
        "         &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov3.5 &8┃ &b&o■",
        "     &3&lMaintenance &8&l» &7We are still in &3&lmaintenance",
        false,
        1,
        new String[]{
          " ",
          "&b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&ov3.5 &8┃ &b&o■",
          "&7Discord &8&l» &bdiscord.cloudnetservice.eu",
          " "
        },
        "&8➜ &c&lMaintenance &8┃ &c✘"
      ))
    );
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(@NotNull String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public boolean isMaintenance() {
    return this.maintenance;
  }

  public void setMaintenance(boolean maintenance) {
    this.maintenance = maintenance;
  }

  public int getMaxPlayers() {
    return this.maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public @NotNull Set<String> getWhitelist() {
    return this.whitelist;
  }

  public void setWhitelist(@NotNull Set<String> whitelist) {
    this.whitelist = whitelist;
  }

  public @NotNull List<SyncProxyMotd> getMotds() {
    return this.motds;
  }

  public void setMotds(@NotNull List<SyncProxyMotd> motds) {
    this.motds = motds;
  }

  public @NotNull List<SyncProxyMotd> getMaintenanceMotds() {
    return this.maintenanceMotds;
  }

  public void setMaintenanceMotds(@NotNull List<SyncProxyMotd> maintenanceMotds) {
    this.maintenanceMotds = maintenanceMotds;
  }

}
