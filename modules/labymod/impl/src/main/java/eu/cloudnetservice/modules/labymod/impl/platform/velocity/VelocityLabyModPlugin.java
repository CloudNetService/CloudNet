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

package eu.cloudnetservice.modules.labymod.impl.platform.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.impl.platform.PlatformLabyModListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "velocity",
  name = "CloudNet-LabyMod",
  authors = "CloudNetService",
  version = "@version@",
  description = "Displays LabyMod DiscordRPC information when playing on cloudnet a server",
  dependencies = @Dependency(name = "CloudNet-Bridge")
)
public class VelocityLabyModPlugin implements PlatformEntrypoint {

  private final EventManager eventManager;
  private final ModuleHelper moduleHelper;

  @Inject
  public VelocityLabyModPlugin(@NonNull EventManager eventManager, @NonNull ModuleHelper moduleHelper) {
    this.eventManager = eventManager;
    this.moduleHelper = moduleHelper;
  }

  @Inject
  public void registerPlatformListener(
    @NonNull PluginContainer plugin,
    @NonNull VelocityLabyModListener listener,
    @NonNull com.velocitypowered.api.event.EventManager eventManager
  ) {
    eventManager.register(plugin, listener);
  }

  @Inject
  public void registerPluginChannel(@NonNull ChannelRegistrar channelRegistrar) {
    channelRegistrar.register(new LegacyChannelIdentifier(LabyModManagement.LABYMOD_CLIENT_CHANNEL));
  }

  @Override
  public void onLoad() {
    // register the common cloudnet listener for channel messages
    this.eventManager.registerListener(PlatformLabyModListener.class);
  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
