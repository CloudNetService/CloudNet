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

package de.dytanic.cloudnet.ext.syncproxy;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;

public class SyncProxyCloudNetListener {

  private final AbstractSyncProxyManagement syncProxyManagement;

  public SyncProxyCloudNetListener(AbstractSyncProxyManagement syncProxyManagement) {
    this.syncProxyManagement = syncProxyManagement;
  }

  @EventListener
  public void handle(CloudServicePostLifecycleEvent event) {
    if (event.getNewLifeCycle() == ServiceLifeCycle.RUNNING) {
      this.syncProxyManagement.broadcastServiceStateChange("service-start", event.getServiceInfo());
    } else if (event.getNewLifeCycle() == ServiceLifeCycle.STOPPED) {
      ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();
      if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftProxy() && this.syncProxyManagement
        .inGroup(serviceInfoSnapshot)) {
        this.syncProxyManagement.removeServiceOnlineCount(serviceInfoSnapshot);
      }

      this.syncProxyManagement.broadcastServiceStateChange("service-stop", serviceInfoSnapshot);
    }
  }

  @EventListener
  public void handleUpdate(CloudServiceUpdateEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftProxy() && this.syncProxyManagement
      .inGroup(serviceInfoSnapshot)) {
      this.syncProxyManagement.updateServiceOnlineCount(serviceInfoSnapshot);
    }
  }
}
