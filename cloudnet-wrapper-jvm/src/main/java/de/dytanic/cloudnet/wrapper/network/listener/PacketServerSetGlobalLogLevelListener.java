package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;

public class PacketServerSetGlobalLogLevelListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        CloudNetDriver.getInstance().getLogger().setLevel(packet.getBody().readInt());
    }

}
