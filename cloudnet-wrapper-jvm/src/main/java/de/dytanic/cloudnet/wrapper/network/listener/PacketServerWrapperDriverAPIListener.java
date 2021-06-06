package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;

public class PacketServerWrapperDriverAPIListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    DriverAPIRequestType requestType = packet.getBuffer().readEnumConstant(DriverAPIRequestType.class);

    if (requestType == DriverAPIRequestType.FORCE_UPDATE_SERVICE) {
      ServiceInfoSnapshot serviceInfoSnapshot = Wrapper.getInstance().configureServiceInfoSnapshot();
      channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObject(serviceInfoSnapshot)));
    }
  }

}
