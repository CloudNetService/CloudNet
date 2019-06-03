package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;

public final class PacketServerDeployLocalTemplateListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("command") && packet.getHeader().getString("command").equalsIgnoreCase("deploy_template") &&
                packet.getHeader().contains("serviceTemplate")) {
            ITemplateStorage storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

            storage.deploy(packet.getBody(), packet.getHeader().get("serviceTemplate", ServiceTemplate.class));
        }
    }
}