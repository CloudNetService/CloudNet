package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class ProcessingServiceVersionInstaller implements ServiceVersionInstaller {

    @Override
    public void install(ServiceVersion version, Path workingDirectory, OutputStream targetStream) throws IOException {
        String command = version.getProperties().getString("command");
        String copy = version.getProperties().getString("copy");

        if (command == null || copy == null) {
            throw new IllegalStateException(String.format("Missing command or copy property on service version %s!", version.getName()));
        }

        this.download(version.getUrl(), workingDirectory.resolve("download.jar"));

        Process process = Runtime.getRuntime().exec(command, null, workingDirectory.toFile());
        try {
            process.waitFor();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        Pattern pattern = Pattern.compile(copy);

        Files.walkFileTree(workingDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                Path relativePath = workingDirectory.relativize(filePath);

                if (pattern.matcher(relativePath.toString()).matches()) {
                    Files.copy(relativePath, targetStream);
                    return FileVisitResult.TERMINATE;
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void download(String url, Path targetFile) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        connection.connect();

        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, targetFile);
        }

        connection.disconnect();
    }
}
