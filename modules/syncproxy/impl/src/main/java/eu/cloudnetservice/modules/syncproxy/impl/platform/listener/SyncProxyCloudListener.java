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

package eu.cloudnetservice.modules.syncproxy.impl.platform.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConstants;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.modules.syncproxy.impl.platform.PlatformSyncProxyManagement;
import lombok.NonNull;

public record SyncProxyCloudListener<P>(@NonNull PlatformSyncProxyManagement<P> management) {

  @EventListener
  public void handleServiceLifecycleChange(@NonNull CloudServiceLifecycleChangeEvent event) {
    switch (event.newLifeCycle()) {
      // notify the players about a new service start
      case RUNNING -> this.notifyPlayers("service-start", event.serviceInfo());
      case STOPPED, DELETED -> {
        // notify the players about the service stop
        this.notifyPlayers("service-stop", event.serviceInfo());
        // remove the ServiceInfoSnapshot from the cache as the service is stopping
        this.management.removeCachedServiceInfoSnapshot(event.serviceInfo());
      }
      default -> {
      }
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
      this.management.configurationSilently(event.content().readObject(SyncProxyConfiguration.class));
    }
  }

  private void notifyPlayers(@NonNull String key, @NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    // only message the players if we are supposed to
    if (this.management.configuration().ingameServiceStartStopMessages()) {
      for (var onlinePlayer : this.management.onlinePlayers()) {
        if (this.management.checkPlayerPermission(onlinePlayer, "cloudnet.syncproxy.notify")) {
          this.management.messagePlayer(onlinePlayer, this.management.serviceUpdateMessage(key, serviceInfoSnapshot));
        }
      }
    }
  }
}
