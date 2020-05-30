package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;

public final class PacketClientServiceInfoUpdateListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ServiceInfoSnapshot serviceInfoSnapshot = packet.getBody().readObject(ServiceInfoSnapshot.class);

        ICloudServiceManager cloudServiceManager = CloudNet.getInstance().getCloudServiceManager();
        ICloudService cloudService = cloudServiceManager.getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

        if (cloudService != null) {
            cloudService.updateServiceInfoSnapshot(serviceInfoSnapshot);
        }
    }
}