package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.CloudNet;
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

public class ProcessingServiceVersionInstaller extends ServiceVersionInstaller {

    @Override
    public void install(ServiceVersion version, Path workingDirectory, OutputStream targetStream) throws IOException {
        String copy = version.getProperties().getString("copy");
        String[] parameters = version.getProperties().get("parameters", String[].class);

        if (copy == null) {
            throw new IllegalStateException(String.format("Missing copy property on service version %s!", version.getName()));
        }

        this.download(version.getUrl(), workingDirectory.resolve("download.jar"));

        String command = CloudNet.getInstance().getConfig().getJVMCommand() + " -jar download.jar";
        if (parameters != null) {
            command += " " + String.join(" ", parameters);
        }
        command = command.replace("%version%", version.getName());
        try {
            int exitCode = this.startProcessAndWaitFor(command, workingDirectory);
            if (exitCode != 0) {
                throw new IllegalStateException("ExitCode was " + exitCode + ", not 0");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Pattern pattern = Pattern.compile(copy);

        Files.walkFileTree(workingDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Path relativePath = workingDirectory.relativize(path);

                if (pattern.matcher(relativePath.toString().replace("\\", "/")).matches()) {
                    Files.copy(path, targetStream);
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
