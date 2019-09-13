package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

import java.util.UUID;

public final class PacketClusterSyncAPIPacketListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        handle0(channel, packet);
    }

    private void handle0(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains(PacketConstants.SYNC_PACKET_ID_PROPERTY) && packet.getHeader().contains(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY) &&
                packet.getHeader().getString(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY).equals(PacketConstants.CLUSTER_NODE_SYNC_PACKET_CHANNEL_NAME)) {
            if ("get_reserved_task_ids".equals(packet.getHeader().getString(PacketConstants.SYNC_PACKET_ID_PROPERTY))) {
                this.sendResponse(channel, packet.getUniqueId(),
                        new JsonDocument("taskIds", CloudNet.getInstance().getCloudServiceManager().getReservedTaskIds(
                                packet.getHeader().getString("task")
                        )),
                        new byte[0]
                );
            }
        }
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header, byte[] body) {
        channel.sendPacket(new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, uniqueId, header, body));
    }

    private void sendEmptyResponse(INetworkChannel channel, UUID uniqueId) {
        this.sendResponse(channel, uniqueId, new JsonDocument(), new byte[0]);
    }
}