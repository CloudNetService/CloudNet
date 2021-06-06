package de.dytanic.cloudnet.wrapper.network.packet;

import de.dytanic.cloudnet.driver.api.RemoteDatabaseRequestType;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.function.Consumer;

public class PacketClientDatabaseAction extends Packet {

  public PacketClientDatabaseAction(RemoteDatabaseRequestType type, Consumer<ProtocolBuffer> modifier) {
    super(PacketConstants.INTERNAL_DATABASE_API_CHANNEL, ProtocolBuffer.create().writeEnumConstant(type));
    if (modifier != null) {
      modifier.accept(super.body);
    }
  }

}
