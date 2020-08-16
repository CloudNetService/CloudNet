package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;

public final class PacketServerDeployLocalTemplateListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        TemplateStorage storage = CloudNetDriver.getInstance().getLocalTemplateStorage();

        ServiceTemplate template = packet.getBuffer().readObject(ServiceTemplate.class);
        boolean preClear = packet.getBuffer().readBoolean();

        if (preClear) {
            storage.delete(template);
        }

        byte[] data = packet.getBuffer().readArray();
        storage.deploy(data, template);
    }

}