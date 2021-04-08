package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

final class NetworkChannelHandlerUtils {

    private NetworkChannelHandlerUtils() {
        throw new UnsupportedOperationException();
    }

    static boolean handleInitChannel(INetworkChannel channel, ChannelType channelType) {
        NetworkChannelInitEvent networkChannelInitEvent = new NetworkChannelInitEvent(channel, channelType);
        CloudNetDriver.getInstance().getEventManager().callEvent(networkChannelInitEvent);

        if (networkChannelInitEvent.isCancelled()) {
            try {
                channel.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }

        return true;
    }

    public static void closeNodeServer(IClusterNodeServer clusterNodeServer) {
        try {
            clusterNodeServer.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}