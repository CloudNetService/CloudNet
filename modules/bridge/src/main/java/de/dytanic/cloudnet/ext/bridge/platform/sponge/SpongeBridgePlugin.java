/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.bridge.platform.sponge;

import com.google.inject.Inject;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_bridge")
public final class SpongeBridgePlugin {

  private final PluginContainer plugin;

  @Inject
  public SpongeBridgePlugin(@NotNull PluginContainer plugin) {
    this.plugin = plugin;
  }

  @Listener
  public void handle(@NotNull ConstructPluginEvent event) {
    PlatformBridgeManagement<?, ?> management = new SpongeBridgeManagement(Wrapper.getInstance());
    management.registerServices(Wrapper.getInstance().getServicesRegistry());
    management.postInit();
    // register the listener
    Sponge.eventManager().registerListeners(this.plugin, new SpongePlayerManagementListener(this.plugin, management));
  }
}
