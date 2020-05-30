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
    public void handle(INetworkChannel channel, IPacket packet) {
        ITemplateStorage storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

        ServiceTemplate template = packet.getBody().readObject(ServiceTemplate.class);
        boolean preClear = packet.getBody().readBoolean();

        if (preClear) {
            storage.delete(template);
        }

        byte[] data = packet.getBody().readArray();
        storage.deploy(data, template);
    }

}