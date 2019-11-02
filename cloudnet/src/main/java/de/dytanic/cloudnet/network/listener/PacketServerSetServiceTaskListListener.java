package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveServiceTasksUpdateEvent;
import de.dytanic.cloudnet.network.NetworkUpdateType;

import java.util.List;

public final class PacketServerSetServiceTaskListListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("taskList") && packet.getHeader().contains("set")) {
            List<ServiceTask> serviceTasks = packet.getHeader().get("taskList", new TypeToken<List<ServiceTask>>() {
            }.getType());
            NetworkUpdateType updateType = packet.getHeader().get("updateType", NetworkUpdateType.class);

            if (serviceTasks != null) {
                NetworkChannelReceiveServiceTasksUpdateEvent event = new NetworkChannelReceiveServiceTasksUpdateEvent(channel, serviceTasks);
                CloudNetDriver.getInstance().getEventManager().callEvent(event);

                if (!event.isCancelled()) {

                    serviceTasks = event.getServiceTasks() != null ? event.getServiceTasks() : serviceTasks;

                    switch (updateType) {
                        case SET:
                            CloudNet.getInstance().getCloudServiceManager().setServiceTasksWithoutClusterSync(serviceTasks);
                            break;
                        case ADD:
                            for (ServiceTask serviceTask : serviceTasks) {
                                CloudNet.getInstance().getCloudServiceManager().addPermanentServiceTaskWithoutClusterSync(serviceTask);
                            }
                            break;
                        case REMOVE:
                            for (ServiceTask serviceTask : serviceTasks) {
                                CloudNet.getInstance().getCloudServiceManager().removePermanentServiceTaskWithoutClusterSync(serviceTask);
                            }
                            break;
                    }
                }
            }
        }
    }
}