package de.dytanic.cloudnet.ext.bridge.waterdogpe;


import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import dev.waterdog.waterdogpe.network.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.IJoinHandler;
import dev.waterdog.waterdogpe.utils.types.IReconnectHandler;

public class WaterdogPECloudNetReconnectHandler implements IJoinHandler, IReconnectHandler {

    @Override
    public ServerInfo determineServer(ProxiedPlayer player) {
        return WaterdogPECloudNetHelper.getNextFallback(player, null).orElse(null);
    }

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, String kickMessage) {
        BridgeProxyHelper.handleConnectionFailed(player.getUniqueId(), oldServer.getServerName());

        return WaterdogPECloudNetHelper.getNextFallback(player, oldServer).map(serverInfo -> {
            player.sendMessage(kickMessage);
            return serverInfo;
        }).orElse(null);
    }

}
