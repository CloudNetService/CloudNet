package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;

public final class PacketServerSetGlobalServiceInfoList extends Packet {

  public PacketServerSetGlobalServiceInfoList(Collection<ServiceInfoSnapshot> serviceInfoSnapshots) {
    super(PacketConstants.CLUSTER_SERVICE_INFO_LIST_CHANNEL,
      ProtocolBuffer.create().writeObjectCollection(serviceInfoSnapshots));
  }

}
