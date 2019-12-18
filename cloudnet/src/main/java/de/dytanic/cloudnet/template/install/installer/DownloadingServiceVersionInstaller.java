package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class DownloadingServiceVersionInstaller implements ServiceVersionInstaller {

    @Override
    public void install(ServiceVersion version, Path workingDirectory, Callable<OutputStream[]> targetStreamCallable) throws Exception {
        try (InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), new URL(version.getUrl()))) {
            for (OutputStream targetStream : targetStreamCallable.call()) {
                FileUtils.copy(inputStream, targetStream);

                targetStream.close();
            }
        }
    }

}
