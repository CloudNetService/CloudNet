package de.dytanic.cloudnet.network.listener.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.CachedChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketSession;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.ClusterSynchronizedTemplateStorage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PacketServerSyncTemplateStorageChunkListener extends CachedChunkedPacketListener {
    @Override
    protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream) throws IOException {
        ServiceTemplate template = session.getHeader().get("template", ServiceTemplate.class);
        DriverAPIRequestType requestType = session.getHeader().get("type", DriverAPIRequestType.class);

        TemplateStorage templateStorage = CloudNet.getInstance().getTemplateStorage(template.getStorage());
        Preconditions.checkNotNull(templateStorage, "invalid storage %s", template.getStorage());

        if (!(templateStorage instanceof ClusterSynchronizedTemplateStorage)) {
            throw new IllegalArgumentException("TemplateStorage " + template.getStorage() + " is not cluster synchronized");
        }
        ClusterSynchronizedTemplateStorage storage = (ClusterSynchronizedTemplateStorage) templateStorage;

        switch (requestType) {
            case SET_FILE_CONTENT:
                try (OutputStream outputStream = storage.newOutputStreamWithoutSynchronization(template, session.getHeader().getString("path"))) {
                    FileUtils.copy(inputStream, outputStream);
                }
                break;

            case APPEND_FILE_CONTENT:
                try (OutputStream outputStream = storage.appendOutputStreamWithoutSynchronization(template, session.getHeader().getString("path"))) {
                    FileUtils.copy(inputStream, outputStream);
                }
                break;

            case DEPLOY_TEMPLATE_STREAM:
                boolean success = storage.deployWithoutSynchronization(inputStream, template);
                session.getChannel().sendPacket(Packet.createResponseFor(session.getFirstPacket(), ProtocolBuffer.create().writeBoolean(success)));
                break;
        }
    }
}
