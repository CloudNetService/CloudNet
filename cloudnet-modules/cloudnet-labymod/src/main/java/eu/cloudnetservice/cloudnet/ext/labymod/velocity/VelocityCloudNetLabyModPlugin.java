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

package eu.cloudnetservice.cloudnet.ext.labymod.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.velocity.listener.VelocityLabyModListener;

@Plugin(id = "cloudnet_labymod_velocity")
public class VelocityCloudNetLabyModPlugin {

  private final ProxyServer proxyServer;
  private AbstractLabyModManagement labyModManagement;

  @Inject
  public VelocityCloudNetLabyModPlugin(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(ProxyInitializeEvent event) {
    ChannelIdentifier identifier = new LegacyChannelIdentifier(LabyModConstants.LMC_CHANNEL_NAME);
    this.proxyServer.getChannelRegistrar().register(identifier);

    this.labyModManagement = new VelocityLabyModManagement(this.proxyServer, identifier);

    this.proxyServer.getEventManager().register(this, new VelocityLabyModListener(this.labyModManagement));
  }

}
