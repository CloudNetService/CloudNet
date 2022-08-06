/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.cloudperms.velocity.listener.VelocityCloudPermissionsPlayerListener;
import lombok.NonNull;

@Plugin(
  id = "cloudnet_cloudperms",
  name = "CloudNet-CloudPerms",
  version = "{project.build.version}",
  description = "Velocity extension which implement the permission management system from CloudNet into Velocity",
  url = "https://cloudnetservice.eu",
  authors = "CloudNetService"
)
public final class VelocityCloudNetCloudPermissionsPlugin {

  private final ProxyServer proxyServer;

  @Inject
  public VelocityCloudNetCloudPermissionsPlugin(@NonNull ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(@NonNull ProxyInitializeEvent event) {
    this.proxyServer.getEventManager().register(this, new VelocityCloudPermissionsPlayerListener(
      this.proxyServer,
      new VelocityCloudPermissionProvider(CloudNetDriver.instance().permissionManagement()),
      CloudNetDriver.instance().permissionManagement()));
  }

  @Subscribe
  public void handleShutdown(@NonNull ProxyShutdownEvent event) {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
