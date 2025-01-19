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

package eu.cloudnetservice.modules.bridge.impl.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.bridge.impl.platform.waterdog.command.WaterDogPECloudCommand;
import eu.cloudnetservice.modules.bridge.impl.platform.waterdog.command.WaterDogPEHubCommand;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "waterdog",
  name = "CloudNet-Bridge",
  version = "@version@",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  authors = "CloudNetService")
public final class WaterDogPEBridgePlugin implements PlatformEntrypoint {

  private final ProxyServer proxyServer;
  private final ModuleHelper moduleHelper;
  private final ServiceRegistry serviceRegistry;
  private final WaterDogPECloudCommand cloudCommand;
  private final WaterDogPEBridgeManagement bridgeManagement;

  @Inject
  public WaterDogPEBridgePlugin(
    @NonNull ProxyServer proxyServer,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull WaterDogPECloudCommand cloudCommand,
    @NonNull WaterDogPEBridgeManagement bridgeManagement,
    @NonNull WaterDogPEPlayerManagementListener managementListener
  ) {
    this.proxyServer = proxyServer;
    this.moduleHelper = moduleHelper;
    this.serviceRegistry = serviceRegistry;
    this.cloudCommand = cloudCommand;
    this.bridgeManagement = bridgeManagement;
  }

  @Override
  public void onLoad() {
    // init the management
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.bridgeManagement.postInit();

    // register the WaterDog handlers
    var handlers = new WaterDogPEHandlers(this.proxyServer, this.bridgeManagement);
    this.proxyServer.setReconnectHandler(handlers);
    this.proxyServer.setForcedHostHandler(handlers);

    // register the commands
    this.proxyServer.getCommandMap().registerCommand(this.cloudCommand);

    // register the hub command if requested
    if (!this.bridgeManagement.configuration().hubCommandNames().isEmpty()) {
      // convert to an array for easier access
      var names = this.bridgeManagement.configuration().hubCommandNames().toArray(new String[0]);
      // register the command
      this.proxyServer.getCommandMap().registerCommand(new WaterDogPEHubCommand(
        this.bridgeManagement,
        this.proxyServer,
        names[0],
        names.length > 1 ? Arrays.copyOfRange(names, 1, names.length) : new String[0]));
    }
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
