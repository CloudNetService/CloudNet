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
