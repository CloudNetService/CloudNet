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

package eu.cloudnetservice.modules.bridge.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platformlayer.VelocityLayer;
import eu.cloudnetservice.modules.bridge.platform.velocity.commands.VelocityCloudCommand;
import eu.cloudnetservice.modules.bridge.platform.velocity.commands.VelocityHubCommand;
import java.util.Arrays;
import lombok.NonNull;

@Plugin(
  id = "cloudnet_bridge",
  name = "CloudNet-Bridge",
  version = "{project.build.version}",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  url = "https://cloudnetservice.eu",
  authors = "CloudNetService"
)
public final class VelocityBridgePlugin {

  private final ProxyServer proxy;
  private final InjectionLayer<?> injectionLayer;

  @Inject
  public VelocityBridgePlugin(@NonNull ProxyServer proxyServer) {
    this.proxy = proxyServer;
    this.injectionLayer = VelocityLayer.create(proxyServer);
  }

  @Subscribe
  public void handleProxyInit(@NonNull ProxyInitializeEvent event) {
    // init the bridge management
    var management = this.injectionLayer.instance(VelocityBridgeManagement.class);
    management.registerServices(this.injectionLayer.instance(ServiceRegistry.class));
    management.postInit();
    // register the player listeners
    this.proxy.getEventManager().register(this, this.injectionLayer.instance(VelocityPlayerManagementListener.class));
    // register the cloud command
    var velocityCloudCommand = this.injectionLayer.instance(VelocityCloudCommand.class);
    this.proxy.getCommandManager().register("cloudnet", velocityCloudCommand, "cloud");
    // register the hub command if requested
    if (!management.configuration().hubCommandNames().isEmpty()) {
      // convert to an array for easier access
      var names = management.configuration().hubCommandNames().toArray(new String[0]);
      // register the command
      this.proxy.getCommandManager().register(
        names[0],
        new VelocityHubCommand(this.proxy, management),
        names.length > 1 ? Arrays.copyOfRange(names, 1, names.length) : new String[0]);
    }
  }

  @Subscribe
  public void handleProxyShutdown(@NonNull ProxyShutdownEvent event) {
    var moduleHelper = this.injectionLayer.instance(ModuleHelper.class);
    moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
