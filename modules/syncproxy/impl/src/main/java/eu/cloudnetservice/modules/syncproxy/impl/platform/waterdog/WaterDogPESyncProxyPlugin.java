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

package eu.cloudnetservice.modules.syncproxy.impl.platform.waterdog;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.syncproxy.impl.platform.listener.SyncProxyCloudListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "waterdog",
  name = "CloudNet-SyncProxy",
  version = "@version@",
  description = "CloudNet extension which serves proxy utils with CloudNet support",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-Bridge"))
public final class WaterDogPESyncProxyPlugin implements PlatformEntrypoint {

  private final EventManager eventManager;
  private final ModuleHelper moduleHelper;
  private final WaterDogPESyncProxyManagement syncProxyManagement;

  @Inject
  public WaterDogPESyncProxyPlugin(
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull WaterDogPESyncProxyManagement syncProxyManagement
  ) {
    this.eventManager = eventManager;
    this.moduleHelper = moduleHelper;
    this.syncProxyManagement = syncProxyManagement;
  }

  @Override
  public void onLoad() {
    // register the event listener to handle service updates
    this.eventManager.registerListener(new SyncProxyCloudListener<>(this.syncProxyManagement));
  }

  @Inject
  private void registerListener(@NonNull WaterDogPESyncProxyListener listener) {
    // just need to create the instance
  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
