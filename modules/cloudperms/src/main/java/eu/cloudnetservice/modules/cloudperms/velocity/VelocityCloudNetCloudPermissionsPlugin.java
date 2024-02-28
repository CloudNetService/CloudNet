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

package eu.cloudnetservice.modules.cloudperms.velocity;

import com.velocitypowered.api.event.EventManager;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.cloudperms.velocity.listener.VelocityCloudPermissionsPlayerListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "velocity",
  name = "CloudNet-CloudPerms",
  version = "@version@",
  description = "Velocity extension which implement the permission management system from CloudNet into Velocity",
  homepage = "https://cloudnetservice.eu",
  authors = "CloudNetService"
)
public final class VelocityCloudNetCloudPermissionsPlugin implements PlatformEntrypoint {

  private final Object pluginInstance;
  private final ModuleHelper moduleHelper;
  private final EventManager eventManager;
  private final VelocityCloudPermissionsPlayerListener playerListener;

  @Inject
  public VelocityCloudNetCloudPermissionsPlugin(
    @NonNull @Named("plugin") Object pluginInstance,
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull VelocityCloudPermissionsPlayerListener playerListener
  ) {
    this.pluginInstance = pluginInstance;
    this.moduleHelper = moduleHelper;
    this.eventManager = eventManager;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    this.eventManager.register(this.pluginInstance, this.playerListener);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
