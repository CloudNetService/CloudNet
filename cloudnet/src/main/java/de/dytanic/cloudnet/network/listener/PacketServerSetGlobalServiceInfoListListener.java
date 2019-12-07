package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.Collection;
import java.util.List;

public final class PacketServerSetGlobalServiceInfoListListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        if (packet.getHeader().contains("serviceInfoList")) {
            Collection<ServiceInfoSnapshot> serviceInfoSnapshots = packet.getHeader().get("serviceInfoList", new TypeToken<List<ServiceInfoSnapshot>>() {
            }.getType());

            for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
                if (serviceInfoSnapshot != null) {
                    CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots()
                            .put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);
                }
            }
        }
    }
}