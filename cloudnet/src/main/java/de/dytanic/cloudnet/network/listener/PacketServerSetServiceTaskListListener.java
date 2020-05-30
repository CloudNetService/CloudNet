package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveServiceTasksUpdateEvent;
import de.dytanic.cloudnet.network.NetworkUpdateType;

import java.util.ArrayList;
import java.util.List;

public final class PacketServerSetServiceTaskListListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        List<ServiceTask> serviceTasks = new ArrayList<>(packet.getBody().readObjectCollection(ServiceTask.class));
        NetworkUpdateType updateType = packet.getBody().readEnumConstant(NetworkUpdateType.class);

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