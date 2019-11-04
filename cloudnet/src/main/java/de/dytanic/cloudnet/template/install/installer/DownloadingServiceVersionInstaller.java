package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class DownloadingServiceVersionInstaller implements ServiceVersionInstaller {

    @Override
    public void install(ServiceVersion version, Path workingDirectory, Callable<OutputStream[]> targetStreamCallable) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(version.getUrl()).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        connection.connect();

        try (InputStream inputStream = connection.getInputStream()) {
            for (OutputStream targetStream : targetStreamCallable.call()) {
                FileUtils.copy(inputStream, targetStream);

                targetStream.close();
            }
        }

        connection.disconnect();
    }

}
