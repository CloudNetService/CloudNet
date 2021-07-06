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

package de.dytanic.cloudnet.ext.syncproxy.configuration;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SyncProxyProxyLoginConfiguration {

  protected String targetGroup;

  protected boolean maintenance;

  protected int maxPlayers;

  protected List<String> whitelist;

  protected List<SyncProxyMotd> motds;

  protected List<SyncProxyMotd> maintenanceMotds;

  public SyncProxyProxyLoginConfiguration(String targetGroup, boolean maintenance, int maxPlayers,
    List<String> whitelist, List<SyncProxyMotd> motds, List<SyncProxyMotd> maintenanceMotds) {
    this.targetGroup = targetGroup;
    this.maintenance = maintenance;
    this.maxPlayers = maxPlayers;
    this.whitelist = whitelist;
    this.motds = motds;
    this.maintenanceMotds = maintenanceMotds;
  }

  public SyncProxyProxyLoginConfiguration() {
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
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

  public List<String> getWhitelist() {
    return this.whitelist;
  }

  public void setWhitelist(List<String> whitelist) {
    this.whitelist = whitelist;
  }

  public List<SyncProxyMotd> getMotds() {
    return this.motds;
  }

  public void setMotds(List<SyncProxyMotd> motds) {
    this.motds = motds;
  }

  public List<SyncProxyMotd> getMaintenanceMotds() {
    return this.maintenanceMotds;
  }

  public void setMaintenanceMotds(List<SyncProxyMotd> maintenanceMotds) {
    this.maintenanceMotds = maintenanceMotds;
  }

}
