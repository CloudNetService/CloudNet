package de.dytanic.cloudnet.network.listener.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.protocol.chunk.client.CachedChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.client.ChunkedPacketSession;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import org.jetbrains.annotations.NotNull;
import de.dytanic.cloudnet.driver.template.TemplateStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public final class PacketServerDeployLocalTemplateListener extends CachedChunkedPacketListener {

    @Override
    protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream) throws IOException {
        TemplateStorage storage = CloudNetDriver.getInstance().getLocalTemplateStorage();
        ServiceTemplate template = session.getHeader().get("template", ServiceTemplate.class);
        boolean preClear = session.getHeader().getBoolean("preClear");

        if (preClear) {
            storage.delete(template);
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            storage.deploy(zipInputStream, template);
        }
    }

}