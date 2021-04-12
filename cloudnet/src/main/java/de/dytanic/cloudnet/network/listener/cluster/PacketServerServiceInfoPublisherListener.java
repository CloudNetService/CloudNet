package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
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
        packet.getBuffer().markReaderIndex();
        ServiceInfoSnapshot serviceInfoSnapshot = packet.getBuffer().readObject(ServiceInfoSnapshot.class);
        PacketClientServerServiceInfoPublisher.PublisherType publisherType = packet.getBuffer().readEnumConstant(PacketClientServerServiceInfoPublisher.PublisherType.class);

        UUID serviceUniqueId = serviceInfoSnapshot.getServiceId().getUniqueId();

        Map<UUID, ServiceInfoSnapshot> globalServiceInfoSnapshots = this.getGlobalServiceInfoSnapshots();
        boolean knowsService = globalServiceInfoSnapshots.containsKey(serviceUniqueId);

        switch (publisherType) {
            case UPDATE:
                if (!knowsService) {
                    return;
                }

                this.invokeEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().put(serviceUniqueId, serviceInfoSnapshot);
                break;
            case REGISTER:
                this.invokeEvent(new CloudServiceRegisterEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().put(serviceUniqueId, serviceInfoSnapshot);
                break;
            case CONNECTED:
                if (!knowsService) {
                    return;
                }

                this.invokeEvent(new CloudServiceConnectNetworkEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().put(serviceUniqueId, serviceInfoSnapshot);
                break;
            case DISCONNECTED:
                if (!knowsService) {
                    return;
                }

                this.invokeEvent(new CloudServiceDisconnectNetworkEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().put(serviceUniqueId, serviceInfoSnapshot);
                break;
            case UNREGISTER:
                if (!knowsService) {
                    return;
                }

                this.invokeEvent(new CloudServiceUnregisterEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().remove(serviceUniqueId);
                break;
            case STARTED:
                if (!knowsService) {
                    return;
                }

                this.invokeEvent(new CloudServiceStartEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().put(serviceUniqueId, serviceInfoSnapshot);

                System.out.println(LanguageManager.getMessage("cloud-service-pre-start-message-different-node")
                        .replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName())
                        .replace("%serviceId%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()))
                        .replace("%id%", serviceInfoSnapshot.getServiceId().getUniqueId().toString())
                        .replace("%node%", serviceInfoSnapshot.getServiceId().getNodeUniqueId()));
                break;
            case STOPPED:
                if (!knowsService) {
                    return;
                }

                this.invokeEvent(new CloudServiceStopEvent(serviceInfoSnapshot));
                this.getGlobalServiceInfoSnapshots().put(serviceUniqueId, serviceInfoSnapshot);

                System.out.println(LanguageManager.getMessage("cloud-service-pre-stop-message-different-node")
                        .replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName())
                        .replace("%serviceId%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()))
                        .replace("%id%", serviceInfoSnapshot.getServiceId().getUniqueId().toString())
                        .replace("%node%", serviceInfoSnapshot.getServiceId().getNodeUniqueId()));
                break;
        }

        packet.getBuffer().resetReaderIndex();
        this.sendUpdateToAllServices(packet);
    }

    private void invokeEvent(Event event) {
        CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }

    private Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots() {
        return CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots();
    }

    private void sendUpdateToAllServices(IPacket packet) {
        for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packet);
            }
        }
    }
}
