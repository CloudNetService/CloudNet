package de.dytanic.cloudnet.network.listener.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.ClusterSynchronizedTemplateStorage;

import java.io.ByteArrayInputStream;

public class PacketServerSyncTemplateStorageListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        ProtocolBuffer buffer = packet.getBuffer();

        DriverAPIRequestType requestType = buffer.readEnumConstant(DriverAPIRequestType.class);
        ServiceTemplate template = buffer.readObject(ServiceTemplate.class);

        TemplateStorage templateStorage = CloudNet.getInstance().getTemplateStorage(template.getStorage());
        Preconditions.checkNotNull(templateStorage, "invalid storage %s", template.getStorage());

        if (!(templateStorage instanceof ClusterSynchronizedTemplateStorage)) {
            throw new IllegalArgumentException("TemplateStorage " + template.getStorage() + " is not cluster synchronized");
        }
        ClusterSynchronizedTemplateStorage storage = (ClusterSynchronizedTemplateStorage) templateStorage;

        switch (requestType) {
            case DELETE_TEMPLATE:
                storage.deleteWithoutSynchronization(template);
                break;

            case CREATE_TEMPLATE:
                storage.createWithoutSynchronization(template);
                break;

            case CREATE_FILE:
                storage.createFileWithoutSynchronization(template, buffer.readString());
                break;

            case CREATE_DIRECTORY:
                storage.createDirectoryWithoutSynchronization(template, buffer.readString());
                break;

            case DELETE_FILE:
                storage.deleteFileWithoutSynchronization(template, buffer.readString());
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + requestType);
        }
    }
}
