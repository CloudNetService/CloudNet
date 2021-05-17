package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.CachedChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketSession;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.ClusterSynchronizedTemplateStorage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public final class PacketServerDeployLocalTemplateListener extends CachedChunkedPacketListener {

    @Override
    protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream) throws IOException {
        TemplateStorage storage = CloudNetDriver.getInstance().getLocalTemplateStorage();
        ServiceTemplate template = session.getHeader().get("template", ServiceTemplate.class);
        boolean preClear = session.getHeader().getBoolean("preClear");

        try {
            this.deployToStorage(storage, template, inputStream, preClear);
        } finally {
            inputStream.close();
        }
    }

    protected void deployToStorage(TemplateStorage templateStorage, ServiceTemplate template, InputStream stream, boolean preClear) {
        if (templateStorage instanceof ClusterSynchronizedTemplateStorage) {
            ClusterSynchronizedTemplateStorage synchronizedStorage = (ClusterSynchronizedTemplateStorage) templateStorage;
            if (preClear) {
                synchronizedStorage.deleteWithoutSynchronization(template);
            }

            synchronizedStorage.deployWithoutSynchronization(stream, template);
        } else {
            if (preClear) {
                templateStorage.delete(template);
            }

            templateStorage.deploy(stream, template);
        }
    }
}