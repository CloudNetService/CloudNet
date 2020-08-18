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

public class PacketServerSyncTemplateStorageListener implements IPacketListener {

    private final boolean redirectToCluster;

    public PacketServerSyncTemplateStorageListener(boolean redirectToCluster) {
        this.redirectToCluster = redirectToCluster;
    }

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
            case DEPLOY_TEMPLATE_BYTE_ARRAY:
                if (this.redirectToCluster) {
                    storage.deploy(buffer.readArray(), template);
                } else {
                    storage.deployWithoutSynchronization(buffer.readArray(), template);
                }
                break;

            case DELETE_TEMPLATE:
                if (this.redirectToCluster) {
                    storage.delete(template);
                } else {
                    storage.deleteWithoutSynchronization(template);
                }
                break;

            case CREATE_TEMPLATE:
                if (this.redirectToCluster) {
                    storage.create(template);
                } else {
                    storage.createWithoutSynchronization(template);
                }
                break;

            case CREATE_FILE:
                if (this.redirectToCluster) {
                    storage.createFile(template, buffer.readString());
                } else {
                    storage.createFileWithoutSynchronization(template, buffer.readString());
                }
                break;

            case CREATE_DIRECTORY:
                if (this.redirectToCluster) {
                    storage.createDirectory(template, buffer.readString());
                } else {
                    storage.createDirectoryWithoutSynchronization(template, buffer.readString());
                }
                break;

            case DELETE_FILE:
                if (this.redirectToCluster) {
                    storage.deleteFile(template, buffer.readString());
                } else {
                    storage.deleteFileWithoutSynchronization(template, buffer.readString());
                }
                break;
        }

    }
}
