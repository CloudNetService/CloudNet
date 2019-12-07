package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.event.service.CloudServiceConsoleLogReceiveEntryEvent;

public final class PacketServerConsoleLogEntryReceiveListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        if (packet.getHeader().contains("command") && packet.getHeader().getString("command").equals("log_entry_receive")) {
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceConsoleLogReceiveEntryEvent(
                    packet.getHeader().get("serviceInfoSnapshot", ServiceInfoSnapshot.TYPE),
                    packet.getHeader().getString("messageEntry"),
                    packet.getHeader().getBoolean("errorMessage")
            ));
        }
    }
}