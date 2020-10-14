package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public interface DriverAPIHandler {

    ProtocolBuffer handle(INetworkChannel channel, IPacket packet, ProtocolBuffer buffer);

}
