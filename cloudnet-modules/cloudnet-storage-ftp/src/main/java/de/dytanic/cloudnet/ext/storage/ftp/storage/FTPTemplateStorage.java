package de.dytanic.cloudnet.ext.storage.ftp.storage;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FTPTemplateStorage extends AbstractFTPStorage {

    private static final LogLevel LOG_LEVEL = new LogLevel("ftp", "FTP", 1, true);

    private final FTPClient ftpClient;

    public FTPTemplateStorage(String name, FTPCredentials credentials, boolean ssl) {
        super(name, credentials, ssl ? FTPType.FTPS : FTPType.FTP);

        this.ftpClient = ssl ? new FTPSClient() : new FTPClient();
    }

    @Override
    public boolean connect() {
        return this.connect(super.credentials.getAddress().getHost(), super.credentials.getUsername(), super.credentials.getPassword(), super.credentials.getAddress().getPort());
    }

    private boolean connect(String host, String username, String password, int port) {
        if (this.ftpClient.isConnected()) {
            try {
                this.ftpClient.disconnect();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        ILogger logger = CloudNetDriver.getInstance().getLogger();

        try {
            this.ftpClient.setAutodetectUTF8(true);

            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect")
                    .replace("%host%", host)
                    .replace("%port%", String.valueOf(port))
                    .replace("%ftpType%", this.ftpType.toString())
            );
            this.ftpClient.connect(host, port);

            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-login")
                    .replace("%user%", username)
                    .replace("%ftpType%", this.ftpType.toString())
            );
            this.ftpClient.login(username, password);

            this.ftpClient.sendNoOp();
            this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            this.ftpClient.changeWorkingDirectory(super.baseDirectory);

            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return this.ftpClient.isAvailable();
    }

    @Override
    public void close() throws IOException {
        this.ftpClient.disconnect();
    }

    @Override
    public boolean deploy(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        Validate.checkNotNull(zipInput);
        Validate.checkNotNull(target);

        if (this.has(target)) {
            this.delete(target);
        }

        this.create(target);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipInput);
             ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream, StandardCharsets.UTF_8)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                this.deployZipEntry(zipInputStream, zipEntry, target.getTemplatePath());
                zipInputStream.closeEntry();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private void deployZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, String targetDirectory) throws IOException {
        String entryPath = (targetDirectory + "/" + zipEntry.getName()).replace(File.separatorChar, '/');

        if (zipEntry.isDirectory()) {
            this.createDirectories(entryPath);
        } else {
            this.createParent(entryPath);
            this.ftpClient.storeFile(entryPath, zipInputStream);
        }
    }

    @Override
    public boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter) {
        Validate.checkNotNull(directory);
        Validate.checkNotNull(target);

        if (directory.exists()) {
            File[] files = directory.listFiles();

            boolean value = false;

            if (files != null) {
                value = this.deploy(files, target, fileFilter);
            }

            return value;
        }

        return false;
    }

    @Override
    public boolean deploy(@NotNull Path[] paths, @NotNull ServiceTemplate target) {
        Validate.checkNotNull(paths);
        Validate.checkNotNull(target);

        return this.deploy(Arrays.stream(paths).map(Path::toFile).toArray(File[]::new), target);
    }

    @Override
    public boolean deploy(@NotNull File[] files, @NotNull ServiceTemplate target) {
        return this.deploy(files, target, null);
    }

    private boolean deploy(File[] files, ServiceTemplate target, Predicate<File> fileFilter) {
        Validate.checkNotNull(files);
        Validate.checkNotNull(target);

        if (this.has(target)) {
            this.delete(target);
        }

        try {
            this.createDirectories(target.getTemplatePath());

            for (File file : files) {
                if (file != null && (fileFilter == null || fileFilter.test(file))) {
                    this.deployFile(file, target.getTemplatePath());
                }
            }

            return true;

        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private void deployFile(File file, String targetDirectory) throws IOException {
        if (file.isDirectory()) {
            this.createDirectories(targetDirectory + "/" + file.getName());

            File[] files = file.listFiles();

            if (files != null) {
                for (File entry : files) {
                    this.deployFile(entry, targetDirectory + "/" + file.getName());
                }
            }

        } else {
            try (InputStream inputStream = new FileInputStream(file)) {
                this.ftpClient.storeFile(targetDirectory + "/" + file.getName(), inputStream);
            }
        }
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File directory) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directory);

        if (!this.has(template)) {
            return false;
        }

        directory.mkdirs();

        try {
            FTPFile[] files = this.ftpClient.listFiles(template.getTemplatePath());

            if (files != null) {
                for (FTPFile file : files) {
                    this.copyFile(template.getTemplatePath() + "/" + file.getName(), file, directory);
                }
            }
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    private void copyFile(String filePath, FTPFile file, File directory) throws IOException {
        directory.mkdirs();

        if (file.isDirectory()) {

            FTPFile[] files = this.ftpClient.listFiles(filePath);

            if (files != null) {
                for (FTPFile entry : files) {
                    this.copyFile(filePath + "/" + entry.getName(), entry, new File(directory, file.getName()));
                }
            }

        } else if (file.isFile()) {
            File entry = new File(directory, file.getName());
            entry.getParentFile().mkdirs();

            if (!entry.exists()) {
                entry.createNewFile();

                try (OutputStream outputStream = new FileOutputStream(entry)) {
                    this.ftpClient.retrieveFile(filePath, outputStream);
                }
            }
        }
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directory);

        return this.copy(template, directory.toFile());
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File[] directories) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directories);

        boolean value = true;

        for (File dir : directories) {
            if (!this.copy(template, dir)) {
                value = false;
            }
        }

        return value;
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path[] directories) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directories);

        return this.copy(template, Arrays.stream(directories).map(Path::toFile).toArray(File[]::new));
    }

    @Override
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        if (!this.has(template)) {
            return FileUtils.emptyZipByteArray();
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {

            this.toByteArray(zipOutputStream, template.getTemplatePath(), "");

            return byteArrayOutputStream.toByteArray();

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return FileUtils.emptyZipByteArray();
    }

    private void toByteArray(ZipOutputStream zipOutputStream, String baseDirectory, String relativeDirectory) throws IOException {
        FTPFile[] files = this.ftpClient.listFiles(baseDirectory);

        if (files != null) {
            for (FTPFile file : files) {
                if (file != null) {
                    if (file.isDirectory()) {
                        this.toByteArray(zipOutputStream, baseDirectory + "/" + file.getName(),
                                relativeDirectory + (relativeDirectory.isEmpty() ? "" : "/") + file.getName());
                    } else if (file.isFile()) {
                        zipOutputStream.putNextEntry(new ZipEntry(relativeDirectory + (relativeDirectory.isEmpty() ? "" : "/") + file.getName()));
                        this.ftpClient.retrieveFile(baseDirectory + "/" + file.getName(), zipOutputStream);
                        zipOutputStream.closeEntry();
                    }
                }
            }
        }
    }

    @Override
    public boolean delete(@NotNull ServiceTemplate template) {
        Validate.checkNotNull(template);

        try {
            this.deleteDir(template.getTemplatePath());
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        Validate.checkNotNull(template);

        try {
            this.createDirectories(template.getTemplatePath());
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private void deleteDir(String path) throws IOException {
        for (FTPFile ftpFile : this.ftpClient.mlistDir(path)) {
            String filePath = path + "/" + ftpFile.getName();

            if (ftpFile.isDirectory()) {
                this.deleteDir(filePath);
            } else {
                this.ftpClient.deleteFile(filePath);
            }
        }

        this.ftpClient.removeDirectory(path);
    }

    @Override
    public boolean has(@NotNull ServiceTemplate template) {
        Validate.checkNotNull(template);

        try {
            return this.ftpClient.listFiles(template.getTemplatePath()).length > 0;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Nullable
    @Override
    public OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        String fullPath = template.getTemplatePath() + "/" + path;

        this.createParent(fullPath);
        return this.ftpClient.appendFileStream(fullPath);
    }

    @Nullable
    @Override
    public OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        String fullPath = template.getTemplatePath() + "/" + path;

        this.createParent(fullPath);
        return this.ftpClient.storeFileStream(fullPath);
    }

    @Override
    public void completeDataTransfer() {
        try {
            this.ftpClient.completePendingCommand();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        String fullPath = template.getTemplatePath() + "/" + path;

        this.createParent(fullPath);
        return this.ftpClient.storeFile(fullPath, new ByteArrayInputStream(new byte[0]));
    }

    private void createParent(String path) throws IOException {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int slash = path.lastIndexOf('/');

        if (slash > 0) {
            this.createDirectories(path.substring(0, slash));
        }
    }

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        this.createDirectories(template.getTemplatePath() + "/" + path);
        return true;
    }

    @Override
    public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        FTPFile file = this.ftpClient.mlistFile(template.getTemplatePath() + "/" + path);
        return file != null;
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.ftpClient.deleteFile(template.getTemplatePath() + "/" + path);
    }

    @Override
    public String[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir) throws IOException {
        FTPFile[] fileList = this.ftpClient.mlistDir(template.getTemplatePath() + "/" + dir);
        return fileList == null ? new String[0] : Arrays.stream(fileList).map(FTPFile::getName).toArray(String[]::new);
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        Collection<ServiceTemplate> templates = new ArrayList<>();

        try {
            FTPFile[] files = this.ftpClient.listFiles();

            if (files != null) {
                for (FTPFile entry : files) {
                    if (entry.isDirectory()) {
                        FTPFile[] subPathEntries = this.ftpClient.listFiles(entry.getName());

                        if (subPathEntries != null) {
                            for (FTPFile subEntry : subPathEntries) {
                                if (subEntry.isDirectory()) {
                                    templates.add(new ServiceTemplate(entry.getName(), subEntry.getName(), this.getName()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return templates;
    }

    private void createDirectories(String path) throws IOException {
        StringBuilder pathBuilder = new StringBuilder();

        if (this.ftpClient.changeWorkingDirectory(path)) {
            this.ftpClient.changeWorkingDirectory(super.baseDirectory);
            return;
        }

        for (String pathSegment : path.split("/")) {
            pathBuilder.append(pathSegment).append('/');

            String currentPath = pathBuilder.toString();

            if (!this.ftpClient.changeWorkingDirectory(currentPath)) {
                this.ftpClient.makeDirectory(currentPath);
            }

            this.ftpClient.changeWorkingDirectory(super.baseDirectory);
        }

    }

}