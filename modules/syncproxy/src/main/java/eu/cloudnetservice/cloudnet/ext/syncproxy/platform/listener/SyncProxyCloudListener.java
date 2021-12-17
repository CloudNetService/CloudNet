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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyConstants;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.PlatformSyncProxyManagement;
import lombok.NonNull;

public final class SyncProxyCloudListener<P> {

  private final PlatformSyncProxyManagement<P> management;

  public SyncProxyCloudListener(@NonNull PlatformSyncProxyManagement<P> management) {
    this.management = management;
  }

  @EventListener
  public void handleServiceLifecycleChange(@NonNull CloudServiceLifecycleChangeEvent event) {
    if (event.newLifeCycle() == ServiceLifeCycle.RUNNING) {
      // notify the players about a new service start
      this.notifyPlayers("start-service", event.serviceInfo());
    } else if (event.newLifeCycle() == ServiceLifeCycle.STOPPED) {
      // notify the players about the service stop
      this.notifyPlayers("stop-service", event.serviceInfo());
      // remove the ServiceInfoSnapshot from the cache as the service is stopping
      this.management.removeCachedServiceInfoSnapshot(event.serviceInfo());
    }
  }

  @EventListener
  public void handleServiceUpdate(@NonNull CloudServiceUpdateEvent event) {
    // check if the service is not stopping, as this would lead to issues with the CloudServiceLifecycleChangeEvent
    if (event.serviceInfo().lifeCycle() != ServiceLifeCycle.STOPPED) {
      // cache the ServiceInfoSnapshot
      this.management.cacheServiceInfoSnapshot(event.serviceInfo());
    }
  }

  @EventListener
  public void handleConfigUpdate(@NonNull ChannelMessageReceiveEvent event) {
    // handle incoming channel messages on the syncproxy channel
    if (event.channel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL)
      && SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIG.equals(event.message())) {
      // update the configuration locally
      this.management.setConfigurationSilently(event.content().readObject(SyncProxyConfiguration.class));
    }
  }

  private void notifyPlayers(@NonNull String key, @NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    // only message the players if we are supposed to
    if (!this.management.configuration().ingameServiceStartStopMessages()) {
      return;
    }
    for (var onlinePlayer : this.management.onlinePlayers()) {
      if (this.management.checkPlayerPermission(onlinePlayer, "cloudnet.syncproxy.notify")) {
        this.management.messagePlayer(onlinePlayer, this.management.serviceUpdateMessage(key, serviceInfoSnapshot));
      }
    }
  }
}
