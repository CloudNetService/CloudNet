package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;

public final class PacketClientServiceInfoUpdateListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        if (packet.getHeader().contains("message") && packet.getHeader().getString("message").equals("update_serviceInfo") && packet.getHeader().contains("serviceInfoSnapshot")) {
            ServiceInfoSnapshot serviceInfoSnapshot = packet.getHeader().get("serviceInfoSnapshot", ServiceInfoSnapshot.TYPE);

            ICloudServiceManager cloudServiceManager = CloudNet.getInstance().getCloudServiceManager();

            ICloudService cloudService = cloudServiceManager.getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

            if (cloudService != null) {
                cloudService.setServiceInfoSnapshot(serviceInfoSnapshot);
                cloudServiceManager.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);

                CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));

                CloudNet.getInstance().getNetworkClient()
                        .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
                CloudNet.getInstance().getNetworkServer()
                        .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
            }
        }
    }
}