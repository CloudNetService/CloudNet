package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;

public final class PacketServerServiceInfoPublisherListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        packet.getBuffer().markReaderIndex();

        ServiceInfoSnapshot serviceInfoSnapshot = packet.getBuffer().readObject(ServiceInfoSnapshot.class);
        PacketClientServerServiceInfoPublisher.PublisherType publisherType =
                packet.getBuffer().readEnumConstant(PacketClientServerServiceInfoPublisher.PublisherType.class);

        if (CloudNet.getInstance().getCloudServiceManager().handleServiceUpdate(publisherType, serviceInfoSnapshot)) {
            this.publishMessageIfNecessary(publisherType, serviceInfoSnapshot);

            packet.getBuffer().resetReaderIndex();
            this.sendUpdateToAllServices(packet);
        }
    }

    private void publishMessageIfNecessary(PacketClientServerServiceInfoPublisher.PublisherType type, ServiceInfoSnapshot snapshot) {
        switch (type) {
            case STARTED:
                System.out.println(LanguageManager.getMessage("cloud-service-pre-start-message-different-node")
                        .replace("%task%", snapshot.getServiceId().getTaskName())
                        .replace("%serviceId%", String.valueOf(snapshot.getServiceId().getTaskServiceId()))
                        .replace("%id%", snapshot.getServiceId().getUniqueId().toString())
                        .replace("%node%", snapshot.getServiceId().getNodeUniqueId()));
                break;
            case STOPPED:
                System.out.println(LanguageManager.getMessage("cloud-service-pre-stop-message-different-node")
                        .replace("%task%", snapshot.getServiceId().getTaskName())
                        .replace("%serviceId%", String.valueOf(snapshot.getServiceId().getTaskServiceId()))
                        .replace("%id%", snapshot.getServiceId().getUniqueId().toString())
                        .replace("%node%", snapshot.getServiceId().getNodeUniqueId()));
                break;
            default:
                break;
        }
    }

    private void sendUpdateToAllServices(IPacket packet) {
        for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packet);
            }
        }
    }
}
