package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveGroupConfigurationsUpdateEvent;
import java.util.List;

public final class PacketServerSetGroupConfigurationListListener implements
    IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains("groups") && packet.getHeader()
        .contains("set")) {
      List<GroupConfiguration> groupConfigurations = packet.getHeader()
          .get("groups", new TypeToken<List<GroupConfiguration>>() {
          }.getType());

      if (groupConfigurations != null) {
        NetworkChannelReceiveGroupConfigurationsUpdateEvent event = new NetworkChannelReceiveGroupConfigurationsUpdateEvent(
            channel, groupConfigurations);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (!event.isCancelled()) {
          CloudNet.getInstance().getCloudServiceManager()
              .setGroupConfigurations(
                  event.getGroupConfigurations() != null ? event
                      .getGroupConfigurations() : groupConfigurations
              );
        }
      }
    }
  }
}