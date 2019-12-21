package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudService;

public class NodeMessenger implements CloudMessenger {
    private CloudNet cloudNet;

    public NodeMessenger(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Override
    public void sendChannelMessage(String channel, String message, JsonDocument data) {
        Validate.checkNotNull(channel);
        Validate.checkNotNull(message);
        Validate.checkNotNull(data);

        this.cloudNet.sendAll(new PacketClientServerChannelMessage(channel, message, data));
    }

    @Override
    public void sendChannelMessage(ServiceInfoSnapshot targetServiceInfoSnapshot, String channel, String message, JsonDocument data) {
        if (targetServiceInfoSnapshot.getServiceId().getNodeUniqueId().equals(this.cloudNet.getConfig().getIdentity().getUniqueId())) {
            ICloudService cloudService = this.cloudNet.getCloudServiceManager().getCloudService(targetServiceInfoSnapshot.getServiceId().getUniqueId());
            if (cloudService != null && cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(new PacketClientServerChannelMessage(channel, message, data));
            }
        } else {
            IClusterNodeServer nodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(targetServiceInfoSnapshot.getServiceId().getNodeUniqueId());
            if (nodeServer != null) {
                nodeServer.saveSendPacket(new PacketClientServerChannelMessage(
                        targetServiceInfoSnapshot.getServiceId().getUniqueId(),
                        channel,
                        message,
                        data
                ));
            }
        }
    }

    @Override
    public void sendChannelMessage(ServiceTask targetServiceTask, String channel, String message, JsonDocument data) {
        for (ServiceInfoSnapshot serviceInfoSnapshot : this.cloudNet.getCloudServiceProvider().getCloudServices(targetServiceTask.getName())) {
            this.sendChannelMessage(serviceInfoSnapshot, channel, message, data);
        }
    }
}
