package de.dytanic.cloudnet.ext.storage.ftp.storage;

import com.jcraft.jsch.ChannelSftp;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import de.dytanic.cloudnet.ext.storage.ftp.client.SFTPClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

public class SFTPTemplateStorage extends AbstractFTPStorage {

    private final SFTPClient ftpClient;

    public SFTPTemplateStorage(String name, FTPCredentials credentials) {
        super(name, credentials, FTPType.SFTP);

        this.ftpClient = new SFTPClient();
    }

    @Override
    public boolean connect() {
        return this.connect(super.credentials.getAddress().getHost(), super.credentials.getAddress().getPort(), super.credentials.getUsername(), super.credentials.getPassword());
    }

    private boolean connect(String host, int port, String username, String password) {
        if (this.ftpClient.isConnected()) {
            this.ftpClient.close();
        }

        return this.ftpClient.connect(host, port, username, password);
    }

    @Override
    public boolean isAvailable() {
        return this.ftpClient.isConnected();
    }

    @Override
    public void close() {
        if (this.ftpClient != null) {
            this.ftpClient.close();
        }
    }

    @Override
    @Deprecated
    public boolean deploy(byte[] zipInput, @NotNull ServiceTemplate target) {
        return this.ftpClient.uploadDirectory(new ZipInputStream(new ByteArrayInputStream(zipInput), StandardCharsets.UTF_8), this.getPath(target));
    }

    @Override
    public boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target, DirectoryStream.@Nullable Filter<Path> filter) {
        return this.ftpClient.uploadDirectory(directory, this.getPath(target), filter == null ? p -> true : path -> {
            try {
                return filter.accept(path);
            } catch (IOException exception) {
                return false;
            }
        });
    }

    @Override
    public boolean deploy(@NotNull ZipInputStream inputStream, @NotNull ServiceTemplate serviceTemplate) {
        return this.ftpClient.uploadDirectory(inputStream, this.getPath(serviceTemplate));
    }

    @Override
    public boolean deploy(@NotNull Path[] paths, @NotNull ServiceTemplate target) {
        for (Path path : paths) {
            if (!this.ftpClient.uploadFile(path, this.getPath(target) + "/" + path.toString().replace(File.separatorChar, '/'))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
        FileUtils.createDirectoryReported(directory);
        return this.ftpClient.downloadDirectory(this.getPath(template), directory.toString());
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path[] directories) {
        Path tempDirectory = Paths.get(System.getProperty("cloudnet.tempDir.ftpCache", "temp/ftpCache"));
        FileUtils.createDirectoryReported(tempDirectory);

        if (!this.ftpClient.downloadDirectory(this.getPath(template), tempDirectory.toString())) {
            FileUtils.delete(tempDirectory);
            return false;
        }

        for (Path directory : directories) {
            FileUtils.copyFilesToDirectory(tempDirectory, directory);
        }

        FileUtils.delete(tempDirectory);
        return true;
    }

    @Override
    @Deprecated
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.ftpClient.zipDirectory(this.getPath(template), outputStream);
        return outputStream.toByteArray();
    }

    @Override
    @Nullable
    public InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
        if (!this.has(template)) {
            return null;
        }

        Path tempFile = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), UUID.randomUUID().toString());
        try (OutputStream stream = Files.newOutputStream(tempFile)) {
            this.ftpClient.zipDirectory(this.getPath(template), stream);
            return Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
        }
    }

    @Override
    public boolean delete(@NotNull ServiceTemplate template) {
        return this.ftpClient.deleteDirectory(this.getPath(template));
    }

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        this.ftpClient.createDirectories(this.getPath(template));
        return true;
    }

    @Override
    public boolean has(@NotNull ServiceTemplate template) {
        return this.ftpClient.existsDirectory(this.getPath(template));
    }

    @Nullable
    @Override
    public OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
    }

    @Nullable
    @Override
    public OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
    }

    @Override
    public void completeDataTransfer() {
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.ftpClient.createFile(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) {
        this.ftpClient.createDirectories(this.getPath(template) + "/" + path);
        return true;
    }

    @Override
    public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.ftpClient.existsFile(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.ftpClient.deleteFile(this.getPath(template) + "/" + path);
    }

    @Override
    public String[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir) {
        return this.listFiles(this.getPath(template) + "/" + dir).toArray(new String[0]);
    }

    private List<String> listFiles(String directory) {
        List<String> files = new ArrayList<>();
        Collection<ChannelSftp.LsEntry> entries = this.ftpClient.listFiles(directory);
        if (entries != null) {
            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getAttrs().isDir()) {
                    if (directory.endsWith("/") || entry.getFilename().startsWith("/")) {
                        files.addAll(this.listFiles(directory + entry.getFilename()));
                    } else {
                        files.addAll(this.listFiles(directory + "/" + entry.getFilename()));
                    }
                } else {
                    files.add(entry.getFilename());
                }
            }
        }
        return files;
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        Collection<ChannelSftp.LsEntry> entries = this.ftpClient.listFiles(super.baseDirectory);
        if (entries == null) {
            return Collections.emptyList();
        }

        Collection<ServiceTemplate> templates = new ArrayList<>(entries.size());
        for (ChannelSftp.LsEntry entry : entries) {
            String prefix = entry.getFilename();

            Collection<ChannelSftp.LsEntry> prefixEntries = this.ftpClient.listFiles(super.baseDirectory + "/" + prefix);
            if (prefixEntries != null) {
                for (ChannelSftp.LsEntry nameEntry : prefixEntries) {
                    String name = nameEntry.getFilename();

                    templates.add(new ServiceTemplate(prefix, name, this.getName()));
                }
            }
        }

        return templates;
    }

    private String getPath(ServiceTemplate template) {
        return this.baseDirectory + "/" + template.getPrefix() + "/" + template.getName();
    }

}
