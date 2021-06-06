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

package de.dytanic.cloudnet.examples.syncproxy;

import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyMotd;
import java.util.Collections;

/**
 * Examples for plugins running on a server, not on a proxy. Important: You need to compile the SyncProxyModule into
 * your plugin!
 */
public class ServerSPExample {

  private final SyncProxyConfiguration syncProxyConfiguration = SyncProxyConfiguration.getConfigurationFromNode();

  /**
   * Actives maintenance for the given group
   *
   * @param proxyGroup the group to activate maintenance for
   */
  public void changeMaintenance(String proxyGroup) {
    if (this.syncProxyConfiguration != null) {
      // getting the wanted LoginConfiguration of the target proxyGroup
      this.syncProxyConfiguration.getLoginConfigurations()
        .stream()
        .filter(loginConfiguration -> loginConfiguration.getTargetGroup().equalsIgnoreCase(proxyGroup))
        .findFirst()
        .ifPresent(loginConfiguration -> {
          loginConfiguration.setMaintenance(true);

          // updating in cluster
          SyncProxyConfiguration.updateSyncProxyConfigurationInNetwork(this.syncProxyConfiguration);
        });
    }
  }

  /**
   * Changes the MOTD for the group of the current proxy
   */
  public void changeMOTD(String proxyGroup) {
    if (this.syncProxyConfiguration != null) {
      // getting the wanted LoginConfiguration of the target proxyGroup
      this.syncProxyConfiguration.getLoginConfigurations()
        .stream()
        .filter(loginConfiguration -> loginConfiguration.getTargetGroup().equalsIgnoreCase(proxyGroup))
        .findFirst()
        .ifPresent(loginConfiguration -> {
          loginConfiguration.setMotds(Collections.singletonList(
            new SyncProxyMotd("Welcome to my server!",
              "You'll connect to %proxy%",
              true,
              1,
              new String[0],
              null
            )
          ));

          // updating in cluster
          SyncProxyConfiguration.updateSyncProxyConfigurationInNetwork(this.syncProxyConfiguration);
        });
    }
  }

}
