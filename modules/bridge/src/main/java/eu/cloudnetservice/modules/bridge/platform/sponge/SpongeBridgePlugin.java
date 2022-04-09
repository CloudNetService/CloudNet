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

package eu.cloudnetservice.modules.bridge.platform.sponge;

import com.google.inject.Inject;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import lombok.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_bridge")
public final class SpongeBridgePlugin {

  private final PluginContainer plugin;

  @Inject
  public SpongeBridgePlugin(@NonNull PluginContainer plugin) {
    this.plugin = plugin;
  }

  @Listener
  public void handle(@NonNull StartedEngineEvent<Server> event) {
    var management = new SpongeBridgeManagement();
    management.registerServices(Wrapper.instance().serviceRegistry());
    management.postInit();
    // register the listener
    Sponge.eventManager().registerListeners(this.plugin, new SpongePlayerManagementListener(this.plugin, management));
  }
}
