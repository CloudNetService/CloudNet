package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import java.util.Collection;

public final class PacketServerSetGroupConfigurationList extends Packet {

  public PacketServerSetGroupConfigurationList(Collection<GroupConfiguration> groupConfigurations,
    NetworkUpdateType updateType) {
    super(PacketConstants.CLUSTER_GROUP_CONFIG_LIST_CHANNEL,
      ProtocolBuffer.create().writeObjectCollection(groupConfigurations).writeEnumConstant(updateType));
  }
}
