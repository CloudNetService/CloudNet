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

package eu.cloudnetservice.modules.bridge.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.plugin.Plugin;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platformlayer.WaterDogPELayer;
import eu.cloudnetservice.modules.bridge.platform.waterdog.command.WaterDogPECloudCommand;
import eu.cloudnetservice.modules.bridge.platform.waterdog.command.WaterDogPEHubCommand;
import java.util.Arrays;

public final class WaterDogPEBridgePlugin extends Plugin {

  private InjectionLayer<?> injectionLayer;

  @Override
  public void onEnable() {
    this.injectionLayer = WaterDogPELayer.create(this);

    // init the management
    var management = this.injectionLayer.instance(WaterDogPEBridgeManagement.class);
    management.registerServices(this.injectionLayer.instance(ServiceRegistry.class));
    management.postInit();

    // register the listeners (registered during the instance creation due to the weird event system)
    this.injectionLayer.instance(WaterDogPEPlayerManagementListener.class);

    // register the WaterDog handlers
    var handlers = new WaterDogPEHandlers(management);
    ProxyServer.getInstance().setReconnectHandler(handlers);
    ProxyServer.getInstance().setForcedHostHandler(handlers);

    // register the commands
    var waterDogCloudCommand = this.injectionLayer.instance(WaterDogPECloudCommand.class);
    ProxyServer.getInstance().getCommandMap().registerCommand(waterDogCloudCommand);

    // register the hub command if requested
    if (!management.configuration().hubCommandNames().isEmpty()) {
      // convert to an array for easier access
      var names = management.configuration().hubCommandNames().toArray(new String[0]);
      // register the command
      ProxyServer.getInstance().getCommandMap().registerCommand(new WaterDogPEHubCommand(
        management,
        names[0],
        names.length > 1 ? Arrays.copyOfRange(names, 1, names.length) : new String[0]));
    }
  }

  @Override
  public void onDisable() {
    var moduleHelper = this.injectionLayer.instance(ModuleHelper.class);
    moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
