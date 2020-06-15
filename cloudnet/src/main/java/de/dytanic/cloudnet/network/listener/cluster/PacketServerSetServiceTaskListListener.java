package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveServiceTasksUpdateEvent;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;

import java.util.ArrayList;
import java.util.List;

public final class PacketServerSetServiceTaskListListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        List<ServiceTask> serviceTasks = new ArrayList<>(packet.getBuffer().readObjectCollection(ServiceTask.class));
        NetworkUpdateType updateType = packet.getBuffer().readEnumConstant(NetworkUpdateType.class);

        if (updateType == null) {
            return;
        }

        NodeServiceTaskProvider provider = (NodeServiceTaskProvider) CloudNet.getInstance().getServiceTaskProvider();

        NetworkChannelReceiveServiceTasksUpdateEvent event = new NetworkChannelReceiveServiceTasksUpdateEvent(channel, serviceTasks);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (!event.isCancelled()) {

            serviceTasks = event.getServiceTasks() != null ? event.getServiceTasks() : serviceTasks;

            switch (updateType) {
                case SET:
                    provider.setServiceTasksWithoutClusterSync(serviceTasks);
                    break;
                case ADD:
                    for (ServiceTask serviceTask : serviceTasks) {
                        provider.addServiceTaskWithoutClusterSync(serviceTask);
                    }
                    break;
                case REMOVE:
                    for (ServiceTask serviceTask : serviceTasks) {
                        provider.removeServiceTaskWithoutClusterSync(serviceTask.getName());
                    }
                    break;
            }
        }
    }
}