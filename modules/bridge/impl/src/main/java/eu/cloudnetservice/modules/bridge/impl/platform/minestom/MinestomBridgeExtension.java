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

package eu.cloudnetservice.modules.bridge.impl.platform.minestom;

import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ExternalDependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Repository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;
import org.slf4j.Logger;

@Singleton
@PlatformPlugin(
  platform = "minestom",
  name = "CloudNet-Bridge",
  version = "@version@",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  authors = "CloudNetService",
  externalDependencies = @ExternalDependency(
    groupId = "com.google.guava",
    artifactId = "guava",
    version = "31.1-jre",
    repository = @Repository(id = "Central", url = "https://repo1.maven.org/maven2/")))
public final class MinestomBridgeExtension implements PlatformEntrypoint {

  private final Logger logger;
  private final ModuleHelper moduleHelper;
  private final ServiceRegistry serviceRegistry;
  private final MinestomBridgeManagement bridgeManagement;

  @Inject
  public MinestomBridgeExtension(
    @NonNull ComponentLogger logger,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull MinestomBridgeManagement bridgeManagement,
    @NonNull MinestomPlayerManagementListener playerListener
  ) {
    this.logger = logger;
    this.moduleHelper = moduleHelper;
    this.serviceRegistry = serviceRegistry;
    this.bridgeManagement = bridgeManagement;

    serviceRegistry.discoverServices(MinestomBridgeExtension.class);
  }

  @Override
  public void onLoad() {
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.bridgeManagement.postInit();

    // force initialize the bungeecord proxy forwarding
    if (!VelocityProxy.isEnabled()) {
      BungeeCordProxy.enable();
    }

    // using bungeecord and mojang auth will not work, we can't do anything about it. Just send a warning
    if (!VelocityProxy.isEnabled() && MojangAuth.isEnabled()) {
      this.logger.warn(
        "Be aware that using BungeeCord player info forwarding in combination with Mojang authentication will not work!");
    }
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
