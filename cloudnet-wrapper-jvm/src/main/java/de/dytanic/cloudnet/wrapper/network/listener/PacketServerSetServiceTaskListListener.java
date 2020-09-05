package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.driver.event.events.service.task.ServiceTaskRemoveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;

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

        switch (updateType) {
            case ADD:
                for (ServiceTask serviceTask : serviceTasks) {
                    CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskAddEvent(serviceTask));
                }
                break;
            case REMOVE:
                for (ServiceTask serviceTask : serviceTasks) {
                    CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskRemoveEvent(serviceTask));
                }
                break;
        }
    }

}