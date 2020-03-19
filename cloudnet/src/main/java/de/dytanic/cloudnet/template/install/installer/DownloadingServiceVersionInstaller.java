package de.dytanic.cloudnet.template.install.installer;

import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class DownloadingServiceVersionInstaller implements ServiceVersionInstaller {

    @Override
    public void install(ServiceVersion version, String fileName, Path workingDirectory, ITemplateStorage storage, ServiceTemplate targetTemplate, Path cachePath) throws Exception {
        InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), new URL(version.getUrl()));

        if (!version.isLatest()) {
            Files.copy(inputStream, cachePath);
            inputStream.close();

            inputStream = Files.newInputStream(cachePath);
        }

        try (OutputStream outputStream = storage.newOutputStream(targetTemplate, fileName)) {
            ByteStreams.copy(inputStream, Objects.requireNonNull(outputStream, "OutputStream is null!"));
        }

        inputStream.close();
    }

    @Override
    public void shutdown() {
    }

}
