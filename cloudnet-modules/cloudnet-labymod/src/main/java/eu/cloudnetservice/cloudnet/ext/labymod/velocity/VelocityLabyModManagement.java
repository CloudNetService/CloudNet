package eu.cloudnetservice.cloudnet.ext.labymod.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import java.util.UUID;

public class VelocityLabyModManagement extends AbstractLabyModManagement {

  private final ProxyServer proxyServer;
  private final ChannelIdentifier channelIdentifier;

  public VelocityLabyModManagement(ProxyServer proxyServer, ChannelIdentifier channelIdentifier) {
    super(CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class));
    this.proxyServer = proxyServer;
    this.channelIdentifier = channelIdentifier;
  }

  @Override
  protected void connectPlayer(UUID playerId, String target) {
    this.proxyServer.getPlayer(playerId).ifPresent(player ->
      this.proxyServer.getServer(target).ifPresent(registeredServer ->
        player.createConnectionRequest(registeredServer).connect()
      )
    );
  }

  @Override
  protected void sendData(UUID playerId, byte[] data) {
    this.proxyServer.getPlayer(playerId).ifPresent(player -> player.sendPluginMessage(this.channelIdentifier, data));
  }
}
