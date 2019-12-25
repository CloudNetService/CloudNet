package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadingServiceVersionInstaller implements ServiceVersionInstaller {

    @Override
    public void install(ServiceVersion version, String fileName, Path workingDirectory, ITemplateStorage storage, ServiceTemplate targetTemplate, Path cachePath) throws Exception {
        try (InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), new URL(version.getUrl()))) {

            try (OutputStream outputStream = storage.newOutputStream(targetTemplate, fileName)) {
                FileUtils.copy(inputStream, outputStream);
            }

            if (!version.isLatest()) {
                Files.copy(inputStream, cachePath);
            }

        }
    }

}
