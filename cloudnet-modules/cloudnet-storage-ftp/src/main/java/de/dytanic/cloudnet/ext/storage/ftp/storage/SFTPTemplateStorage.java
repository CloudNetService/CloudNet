package de.dytanic.cloudnet.ext.storage.ftp.storage;

import com.jcraft.jsch.ChannelSftp;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import de.dytanic.cloudnet.ext.storage.ftp.client.SFTPClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

public class SFTPTemplateStorage extends GeneralFTPStorage {

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
    public boolean deploy(byte[] zipInput, ServiceTemplate target) {
        return this.ftpClient.uploadDirectory(new ByteArrayInputStream(zipInput), this.getPath(target));
    }

    @Override
    public boolean deploy(File directory, ServiceTemplate target, Predicate<File> fileFilter) {
        return this.ftpClient.uploadDirectory(directory.toPath(), this.getPath(target), path -> fileFilter.test(path.toFile()));
    }

    @Override
    public boolean deploy(Path[] paths, ServiceTemplate target) {
        for (Path path : paths) {
            if (!this.ftpClient.uploadFile(path, this.getPath(target) + "/" + path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deploy(File[] files, ServiceTemplate target) {
        return this.deploy(Arrays.stream(files).map(File::toPath).toArray(Path[]::new), target);
    }

    @Override
    public boolean copy(ServiceTemplate template, File directory) {
        directory.mkdirs();
        return this.ftpClient.downloadDirectory(this.getPath(template), directory.toString());
    }

    @Override
    public boolean copy(ServiceTemplate template, Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return this.ftpClient.downloadDirectory(this.getPath(template), directory.toString());
    }

    @Override
    public boolean copy(ServiceTemplate template, File[] directories) {
        Path tempDirectory = Paths.get(System.getProperty("cloudnet.tempDir.ftpCache", "temp/ftpCache"));
        try {
            Files.createDirectories(tempDirectory);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (!this.ftpClient.downloadDirectory(this.getPath(template), tempDirectory.toString())) {
            FileUtils.delete(tempDirectory.toFile());
            return false;
        }
        for (File directory : directories) {
            try {
                FileUtils.copyFilesToDirectory(tempDirectory.toFile(), directory);
            } catch (IOException exception) {
                exception.printStackTrace();
                FileUtils.delete(tempDirectory.toFile());
                return false;
            }
        }
        FileUtils.delete(tempDirectory.toFile());
        return true;
    }

    @Override
    public boolean copy(ServiceTemplate template, Path[] directories) {
        return this.copy(template, Arrays.stream(directories).map(Path::toFile).toArray(File[]::new));
    }

    @Override
    public byte[] toZipByteArray(ServiceTemplate template) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.ftpClient.zipDirectory(this.getPath(template), outputStream);
        return outputStream.toByteArray();
    }

    @Override
    public boolean delete(ServiceTemplate template) {
        return this.ftpClient.deleteDirectory(this.getPath(template));
    }

    @Override
    public boolean create(ServiceTemplate template) {
        this.ftpClient.createDirectories(this.getPath(template));
        return true;
    }

    @Override
    public boolean has(ServiceTemplate template) {
        return this.ftpClient.existsDirectory(this.getPath(template));
    }

    @Override
    public OutputStream appendOutputStream(ServiceTemplate template, String path) {
        return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
    }

    @Override
    public OutputStream newOutputStream(ServiceTemplate template, String path) {
        return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
    }

    @Override
    public void completeDataTransfer() {
    }

    @Override
    public boolean createFile(ServiceTemplate template, String path) {
        return this.ftpClient.createFile(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean createDirectory(ServiceTemplate template, String path) {
        this.ftpClient.createDirectories(this.getPath(template) + "/" + path);
        return true;
    }

    @Override
    public boolean hasFile(ServiceTemplate template, String path) {
        return this.ftpClient.existsFile(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean deleteFile(ServiceTemplate template, String path) {
        return this.ftpClient.deleteFile(this.getPath(template) + "/" + path);
    }

    @Override
    public String[] listFiles(ServiceTemplate template, String dir) {
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
        if (entries == null)
            return Collections.emptyList();

        Collection<ServiceTemplate> templates = new ArrayList<>(entries.size());

        for (ChannelSftp.LsEntry entry : entries) {
            String prefix = entry.getFilename();

            Collection<ChannelSftp.LsEntry> prefixEntries = this.ftpClient.listFiles(super.baseDirectory + "/" + prefix);
            if (prefixEntries != null) {
                for (ChannelSftp.LsEntry nameEntry : prefixEntries) {
                    String name = nameEntry.getFilename();

                    templates.add(new ServiceTemplate(prefix, name, getName()));
                }
            }
        }

        return templates;
    }

    private String getPath(ServiceTemplate template) {
        return this.baseDirectory + "/" + template.getPrefix() + "/" + template.getName();
    }

}
