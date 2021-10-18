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

package de.dytanic.cloudnet.ext.syncproxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyCloudNetListener;
import de.dytanic.cloudnet.ext.syncproxy.velocity.listener.VelocitySyncProxyPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

@Plugin(id = "cloudnet_syncproxy_velocity")
public final class VelocityCloudNetSyncProxyPlugin {

  private final ProxyServer proxyServer;

  @Inject
  public VelocityCloudNetSyncProxyPlugin(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(ProxyInitializeEvent event) {
    VelocitySyncProxyManagement syncProxyManagement = new VelocitySyncProxyManagement(this.proxyServer, this);
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(AbstractSyncProxyManagement.class, "VelocitySyncProxyManagement", syncProxyManagement);

    CloudNetDriver.getInstance().getEventManager().registerListener(new SyncProxyCloudNetListener(syncProxyManagement));

    this.proxyServer.getEventManager().register(this, new VelocitySyncProxyPlayerListener(syncProxyManagement));
  }

  @Subscribe
  public void handleProxyShutdown(ProxyShutdownEvent event) {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());

    CloudNetDriver.getInstance().getServicesRegistry()
      .unregisterService(AbstractSyncProxyManagement.class, "VelocitySyncProxyManagement");
  }

}
