package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.UUID;

public final class PacketServerChannelMessageListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("channel") && packet.getHeader().contains("message") && packet.getHeader().contains("data")) {
            if (packet.getHeader().contains("uniqueId")) { //this is sent by both the nodes and services
                UUID uniqueId = packet.getHeader().get("uniqueId", UUID.class);
                if (uniqueId != null) {
                    ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceProvider().getCloudService(uniqueId);
                    if (serviceInfoSnapshot != null) {
                        CloudNet.getInstance().getMessenger().sendChannelMessage(
                                serviceInfoSnapshot,
                                packet.getHeader().getString("channel"),
                                packet.getHeader().getString("message"),
                                packet.getHeader().getDocument("data")
                        );
                    }
                }
            } else if (packet.getHeader().contains("task")) { //this is only sent by the services
                ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(packet.getHeader().getString("task"));
                if (serviceTask != null) {
                    CloudNet.getInstance().getMessenger().sendChannelMessage(
                            serviceTask,
                            packet.getHeader().getString("channel"),
                            packet.getHeader().getString("message"),
                            packet.getHeader().getDocument("data")
                    );
                }
            } else { //this can be called from both the nodes and services
                IPacket response = new PacketClientServerChannelMessage(
                        packet.getHeader().getString("channel"),
                        packet.getHeader().getString("message"),
                        packet.getHeader().getDocument("data")
                );
                for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
                    cloudService.getNetworkChannel();
                    cloudService.getNetworkChannel().sendPacket(response);
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(
                        new ChannelMessageReceiveEvent(
                                packet.getHeader().getString("channel"),
                                packet.getHeader().getString("message"),
                                packet.getHeader().getDocument("data")
                        )
                );
            }
        }
    }
}