package eu.cloudnetservice.cloudnet.ext.labymod.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeLabyModManagement extends AbstractLabyModManagement {

  private final ProxyServer proxyServer;

  public BungeeLabyModManagement(ProxyServer proxyServer) {
    super(CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class));
    this.proxyServer = proxyServer;
  }

  @Override
  protected void connectPlayer(UUID playerId, String target) {
    ProxiedPlayer player = this.proxyServer.getPlayer(playerId);
    player.connect(ProxyServer.getInstance().getServerInfo(target));
  }

  @Override
  protected void sendData(UUID playerId, byte[] data) {
    ProxiedPlayer player = this.proxyServer.getPlayer(playerId);
    player.sendData(LabyModConstants.LMC_CHANNEL_NAME, data);
  }
}
