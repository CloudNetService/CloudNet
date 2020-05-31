package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.UUID;

public class PacketClientWrapperSyncListener implements IPacketListener {
    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains(PacketConstants.SYNC_PACKET_ID_PROPERTY) && packet.getHeader().contains(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY) &&
                packet.getHeader().getString(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY).equals("internal_wrapper_channel")) {
            String id = packet.getHeader().getString(PacketConstants.SYNC_PACKET_ID_PROPERTY);
            if ("update_service_info".equals(id)) {
                ServiceInfoSnapshot serviceInfoSnapshot = Wrapper.getInstance().configureServiceInfoSnapshot();
                this.sendResponse(channel, packet.getUniqueId(), JsonDocument.newDocument(serviceInfoSnapshot));
            }
        }
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header) {
        this.sendResponse(channel, uniqueId, header, null);
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header, byte[] body) {
        channel.sendPacket(new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, uniqueId, header, body));
    }

}
