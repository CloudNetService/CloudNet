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

package eu.cloudnetservice.modules.bridge.impl.platform.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.bridge.impl.platform.velocity.commands.VelocityCloudCommand;
import eu.cloudnetservice.modules.bridge.impl.platform.velocity.commands.VelocityHubCommand;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Arrays;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "velocity",
  name = "CloudNet-Bridge",
  version = "@version@",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  authors = "CloudNetService")
public final class VelocityBridgePlugin implements PlatformEntrypoint {

  private final ProxyServer proxy;
  private final Object pluginInstance;

  private final ModuleHelper moduleHelper;
  private final ServiceRegistry serviceRegistry;
  private final VelocityCloudCommand cloudCommand;
  private final VelocityBridgeManagement bridgeManagement;
  private final VelocityPlayerManagementListener playerListener;

  @Inject
  public VelocityBridgePlugin(
    @NonNull @Named("plugin") Object pluginInstance,
    @NonNull ProxyServer proxyServer,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull VelocityCloudCommand cloudCommand,
    @NonNull VelocityBridgeManagement bridgeManagement,
    @NonNull VelocityPlayerManagementListener playerListener
  ) {
    this.pluginInstance = pluginInstance;
    this.proxy = proxyServer;
    this.moduleHelper = moduleHelper;
    this.serviceRegistry = serviceRegistry;
    this.cloudCommand = cloudCommand;
    this.bridgeManagement = bridgeManagement;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    // init the bridge management
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.bridgeManagement.postInit();
    // register the player listeners
    this.proxy.getEventManager().register(this.pluginInstance, this.playerListener);
    // register the cloud command
    this.proxy.getCommandManager().register("cloudnet", this.cloudCommand, "cloud");
    // register the hub command if requested
    if (!this.bridgeManagement.configuration().hubCommandNames().isEmpty()) {
      // convert to an array for easier access
      var names = this.bridgeManagement.configuration().hubCommandNames().toArray(new String[0]);
      // register the command
      this.proxy.getCommandManager().register(
        names[0],
        new VelocityHubCommand(this.proxy, this.bridgeManagement),
        names.length > 1 ? Arrays.copyOfRange(names, 1, names.length) : new String[0]);
    }
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
