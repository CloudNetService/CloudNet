package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.network.ClusterUtils;

public final class PacketServerAuthorizationResponseListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("access")) {
            if (packet.getHeader().getBoolean("access")) {
                for (NetworkClusterNode node : CloudNet.getInstance().getConfig().getClusterConfig().getNodes()) {
                    for (HostAndPort hostAndPort : node.getListeners()) {
                        if (hostAndPort.getPort() == channel.getServerAddress().getPort() &&
                                hostAndPort.getHost().equals(channel.getServerAddress().getHost())) {
                            IClusterNodeServer nodeServer = Iterables.first(CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers(), clusterNodeServer -> clusterNodeServer.getNodeInfo().getUniqueId().equals(node.getUniqueId()));

                            if (nodeServer != null && nodeServer.isAcceptableConnection(channel, node.getUniqueId())) {
                                nodeServer.setChannel(channel);
                                ClusterUtils.sendSetupInformationPackets(channel);

                                CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkChannelAuthClusterNodeSuccessEvent(nodeServer, channel));

                                CloudNet.getInstance().getLogger().info(
                                        LanguageManager.getMessage("cluster-server-networking-connected")
                                                .replace("%id%", node.getUniqueId())
                                                .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                                                .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
                                );
                                break;
                            }
                        }
                    }
                }
            } else {
                CloudNet.getInstance().getLogger().log(LogLevel.WARNING, LanguageManager.getMessage("cluster-server-networking-authorization-failed"));
            }
        }
    }
}