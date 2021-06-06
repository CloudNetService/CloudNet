package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public class PacketServerSetGlobalLogLevel extends Packet {

  public PacketServerSetGlobalLogLevel(int logLevel) {
    super(PacketConstants.INTERNAL_DEBUGGING_CHANNEL, ProtocolBuffer.create().writeInt(logLevel));
  }

}
