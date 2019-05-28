package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveServiceTasksUpdateEvent;
import java.util.List;

public final class PacketServerSetServiceTaskListListener implements
    IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains("taskList") && packet.getHeader()
        .contains("set")) {
      List<ServiceTask> serviceTasks = packet.getHeader()
          .get("taskList", new TypeToken<List<ServiceTask>>() {
          }.getType());

      if (serviceTasks != null) {
        NetworkChannelReceiveServiceTasksUpdateEvent event = new NetworkChannelReceiveServiceTasksUpdateEvent(
            channel, serviceTasks);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (!event.isCancelled()) {
          CloudNet.getInstance().getCloudServiceManager().setServiceTasks(
              event.getServiceTasks() != null ? event.getServiceTasks()
                  : serviceTasks
          );
        }
      }
    }
  }
}