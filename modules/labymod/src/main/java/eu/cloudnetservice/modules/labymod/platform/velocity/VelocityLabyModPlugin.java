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

package eu.cloudnetservice.modules.labymod.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.platform.PlatformLabyModListener;
import eu.cloudnetservice.modules.labymod.platform.PlatformLabyModManagement;
import eu.cloudnetservice.wrapper.Wrapper;
import lombok.NonNull;

@Plugin(
  id = "cloudnet_labymod",
  name = "CloudNet-LabyMod",
  version = "{project.build.version}",
  description = "Displays LabyMod DiscordRPC information when playing on cloudnet a server",
  url = "https://cloudnetservice.eu",
  authors = "CloudNetService",
  dependencies = {
    @Dependency(id = "cloudnet_bridge")
  }
)
public class VelocityLabyModPlugin {

  private final ProxyServer proxy;

  @Inject
  public VelocityLabyModPlugin(@NonNull ProxyServer proxyServer) {
    this.proxy = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(@NonNull ProxyInitializeEvent event) {
    // init the labymod management
    var labyModManagement = new PlatformLabyModManagement();
    // register the plugin channel message listener
    this.proxy.getChannelRegistrar().register(new LegacyChannelIdentifier(LabyModManagement.LABYMOD_CLIENT_CHANNEL));
    this.proxy.getEventManager().register(this, new VelocityLabyModListener(labyModManagement));
    // register the common cloudnet listener for channel messages
    Wrapper.instance().eventManager().registerListener(new PlatformLabyModListener(labyModManagement));
  }

  @Subscribe
  public void handleProxyShutdown(@NonNull ProxyShutdownEvent event) {
    // unregister all listeners for cloudnet events
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
