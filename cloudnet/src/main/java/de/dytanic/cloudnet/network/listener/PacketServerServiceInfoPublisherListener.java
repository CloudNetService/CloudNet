package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.Map;
import java.util.UUID;

public final class PacketServerServiceInfoPublisherListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("serviceInfoSnapshot") && packet.getHeader().contains("type")) {
            ServiceInfoSnapshot serviceInfoSnapshot = packet.getHeader().get("serviceInfoSnapshot", ServiceInfoSnapshot.TYPE);
            PacketClientServerServiceInfoPublisher.PublisherType publisherType = packet.getHeader().get("type", PacketClientServerServiceInfoPublisher.PublisherType.class);

            if (serviceInfoSnapshot == null || publisherType == null) {
                return;
            }

            switch (publisherType) {
                case UPDATE:
                    this.invokeEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                    break;
                case REGISTER:
                    this.invokeEvent(new CloudServiceRegisterEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                    break;
                case CONNECTED:
                    this.invokeEvent(new CloudServiceConnectNetworkEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                    break;
                case DISCONNECTED:
                    this.invokeEvent(new CloudServiceDisconnectNetworkEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                    break;
                case UNREGISTER:
                    this.invokeEvent(new CloudServiceUnregisterEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().remove(serviceInfoSnapshot.getServiceId().getUniqueId());
                    break;
                case STARTED:
                    this.invokeEvent(new CloudServiceStartEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                    break;
                case STOPPED:
                    this.invokeEvent(new CloudServiceStopEvent(serviceInfoSnapshot));
                    this.getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                    break;
            }

            this.sendUpdateToAllServices(serviceInfoSnapshot, publisherType);
        }
    }

    private void invokeEvent(Event event) {
        CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }

    private Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots() {
        return CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots();
    }

    private void sendUpdateToAllServices(ServiceInfoSnapshot serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType type) {
        for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, type));
            }
        }
    }
}