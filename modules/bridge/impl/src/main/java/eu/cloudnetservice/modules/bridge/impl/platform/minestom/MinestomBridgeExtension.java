/*
 * Copyright 2019-present CloudNetService team & contributors
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
import net.minestom.server.timer.SchedulerManager;

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

  private final ModuleHelper moduleHelper;
  private final ServiceRegistry serviceRegistry;
  private final SchedulerManager schedulerManager;
  private final MinestomBridgeManagement bridgeManagement;

  @Inject
  public MinestomBridgeExtension(
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull SchedulerManager schedulerManager,
    @NonNull MinestomBridgeManagement bridgeManagement,
    @NonNull MinestomPlayerManagementListener playerListener
  ) {
    this.moduleHelper = moduleHelper;
    this.serviceRegistry = serviceRegistry;
    this.schedulerManager = schedulerManager;
    this.bridgeManagement = bridgeManagement;

    serviceRegistry.discoverServices(MinestomBridgeExtension.class);
  }

  @Override
  public void onLoad() {
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.schedulerManager.scheduleNextTick(this.bridgeManagement::postInit);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
