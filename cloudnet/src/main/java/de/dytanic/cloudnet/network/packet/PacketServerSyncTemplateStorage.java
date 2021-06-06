package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class PacketServerSyncTemplateStorage extends Packet {

  public PacketServerSyncTemplateStorage(ProtocolBuffer buffer) {
    super(PacketConstants.CLUSTER_TEMPLATE_STORAGE_SYNC_CHANNEL, buffer);
  }
}
