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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.listener.SyncProxyCloudListener;
import org.jetbrains.annotations.NotNull;

@Plugin(
  id = "cloudnet_syncproxy",
  name = "CloudNet-SyncProxy",
  version = "{project.build.version}",
  description = "CloudNet extension which serves proxy utils with CloudNet support",
  url = "https://cloudnetservice.eu",
  authors = "CloudNetService",
  dependencies = {
    @Dependency(id = "cloudnet_bridge"),
    @Dependency(id = "cloudnet_cloudperms", optional = true)
  }
)
public final class VelocitySyncProxyPlugin {

  private final ProxyServer proxyServer;
  private VelocitySyncProxyManagement management;

  @Inject
  public VelocitySyncProxyPlugin(@NotNull ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(@NotNull ProxyInitializeEvent event) {
    this.management = new VelocitySyncProxyManagement(this.proxyServer, this);
    // register the SyncProxyManagement in our service registry
    this.management.registerService(Wrapper.getInstance().servicesRegistry());
    // register the event listener to handle service updates
    Wrapper.getInstance().eventManager().registerListener(new SyncProxyCloudListener<>(this.management));
    // register the velocity ping & join listener
    this.proxyServer.getEventManager().register(this, new VelocitySyncProxyListener(this.management));
  }

  @Subscribe
  public void handleProxyShutdown(@NotNull ProxyShutdownEvent event) {
    // unregister all listeners for cloudnet events
    Wrapper.getInstance().eventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    // remove the service from the registry
    this.management.unregisterService(Wrapper.getInstance().servicesRegistry());
  }

}
