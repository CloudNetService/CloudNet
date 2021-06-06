package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;

public final class PacketServerSetGlobalServiceInfoListListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    Collection<ServiceInfoSnapshot> serviceInfoSnapshots = packet.getBuffer()
      .readObjectCollection(ServiceInfoSnapshot.class);

    for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
      if (serviceInfoSnapshot != null) {
        CloudNet.getInstance().getCloudServiceManager()
          .handleServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType.REGISTER, serviceInfoSnapshot);
      }
    }
  }
}
