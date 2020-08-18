package de.dytanic.cloudnet.ext.storage.ftp.storage;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.ftp.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FTPTemplateStorage extends AbstractFTPStorage {

    private static final LogLevel LOG_LEVEL = new LogLevel("ftp", "FTP", 1, true, true);

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
            this.ftpClient.connect(host, port);

            if (this.ftpClient.login(username, password)) {
                this.ftpClient.sendNoOp();
                this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                this.createDirectories(super.baseDirectory);
                this.ftpClient.changeWorkingDirectory(super.baseDirectory);

                return true;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect-failed")
                .replace("%ftpType%", this.ftpType.toString()));

        return false;
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
    @Deprecated
    public boolean deploy(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        Preconditions.checkNotNull(zipInput);
        Preconditions.checkNotNull(target);

        return this.deploy(new ZipInputStream(new ByteArrayInputStream(zipInput), StandardCharsets.UTF_8), target);
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
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(target);

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
    public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
        Preconditions.checkNotNull(inputStream);
        Preconditions.checkNotNull(target);

        if (this.has(target)) {
            this.delete(target);
        }

        this.create(target);

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        try {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                this.deployZipEntry(zipInputStream, zipEntry, target.getTemplatePath());
                zipInputStream.closeEntry();
            }

            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private boolean deploy(File[] files, ServiceTemplate target, Predicate<File> fileFilter) {
        Preconditions.checkNotNull(files);
        Preconditions.checkNotNull(target);

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
        Preconditions.checkNotNull(template);
        Preconditions.checkNotNull(directory);

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
        Preconditions.checkNotNull(template);
        Preconditions.checkNotNull(directory);

        return this.copy(template, directory.toFile());
    }

    @Override
    @Deprecated
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        if (!this.has(template)) {
            return FileUtils.emptyZipByteArray();
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {

            this.zipToStream(zipOutputStream, template.getTemplatePath(), "");

            return byteArrayOutputStream.toByteArray();

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return FileUtils.emptyZipByteArray();
    }

    @Override
    @Nullable
    public InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
        if (!this.has(template)) {
            return null;
        }

        Path tempFile = FileUtils.createTempFile();

        try (OutputStream stream = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
             ZipOutputStream zipOutputStream = new ZipOutputStream(stream, StandardCharsets.UTF_8)) {
            this.zipToStream(zipOutputStream, template.getTemplatePath(), "");
            return Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
        }
    }

    private void zipToStream(ZipOutputStream zipOutputStream, String baseDirectory, String relativeDirectory) throws IOException {
        FTPFile[] files = this.ftpClient.listFiles(baseDirectory);

        if (files != null) {
            for (FTPFile file : files) {
                if (file != null) {
                    if (file.isDirectory()) {
                        this.zipToStream(zipOutputStream, baseDirectory + "/" + file.getName(),
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
        Preconditions.checkNotNull(template);

        try {
            return this.deleteDir(template.getTemplatePath());
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        Preconditions.checkNotNull(template);
        if (this.has(template)) {
            return false;
        }

        try {
            this.createDirectories(template.getTemplatePath());
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private boolean deleteDir(String path) throws IOException {
        for (FTPFile ftpFile : this.ftpClient.mlistDir(path)) {
            String filePath = path + "/" + ftpFile.getName();

            if (ftpFile.isDirectory()) {
                this.deleteDir(filePath);
            } else {
                this.ftpClient.deleteFile(filePath);
            }
        }

        return this.ftpClient.removeDirectory(path);
    }

    @Override
    public boolean has(@NotNull ServiceTemplate template) {
        Preconditions.checkNotNull(template);

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

    private boolean isDirectory(String fullPath) throws IOException {
        try {
            this.ftpClient.mdtmFile(fullPath);
            return false;
        } catch (MalformedServerReplyException exception) {
            return this.ftpClient.getReplyCode() == FTPReply.FILE_UNAVAILABLE;
        }
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        String fullPath = template.getTemplatePath() + "/" + path;

        return this.isDirectory(fullPath) ? this.deleteDir(fullPath) : this.ftpClient.deleteFile(fullPath);
    }

    @Override
    public @Nullable InputStream newInputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        String fullPath = template.getTemplatePath() + "/" + path;
        return !this.isDirectory(fullPath) ? this.ftpClient.retrieveFileStream(fullPath) : null;
    }

    @Override
    public @Nullable FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        try {
            FTPFile file = this.ftpClient.mdtmFile(template.getTemplatePath() + "/" + path);
            return file == null ? null : this.asInfo(path, file);
        } catch (MalformedServerReplyException exception) {
            if (this.ftpClient.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
                return this.asInfo(path, null);
            }
            return null;
        }
    }

    @Override
    public FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) throws IOException {
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }

        String fullDir = template.getTemplatePath() + "/" + dir;

        if (!this.isDirectory(fullDir)) {
            return null;
        }

        Collection<FileInfo> files = new ArrayList<>();
        try {
            this.listFiles(fullDir, dir, files, deep);
        } catch (MalformedServerReplyException exception) {
            if (this.ftpClient.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
                return null;
            }
            throw exception;
        }
        return files.toArray(new FileInfo[0]);
    }

    private void listFiles(@NotNull String dir, @NotNull String pathPrefix, Collection<FileInfo> files, boolean deep) throws IOException {
        FTPFile[] list = this.ftpClient.listFiles(dir);
        if (list == null) {
            return;
        }

        for (FTPFile file : list) {
            String path = pathPrefix + "/" + file.getName();
            files.add(this.asInfo(path, file));

            if (deep && file.isDirectory()) {
                this.listFiles(dir + "/" + file.getName(), pathPrefix + "/" + file.getName(), files, true);
            }
        }
    }

    private FileInfo asInfo(String path, FTPFile file) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String filename = path;
        int slash = filename.lastIndexOf('/');
        if (slash != -1 && slash < filename.length() - 1) {
            filename = filename.substring(slash + 1);
        }

        return new FileInfo(
                path, filename, file == null || file.isDirectory(),
                false, -1, file == null || file.getTimestamp() == null ? -1 : file.getTimestamp().getTimeInMillis(), -1,
                file == null || file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION), // TODO read and write are always false
                file == null || file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION),
                file == null ? -1 : file.getSize()
        );
    }

    @Override
    public @NotNull Collection<ServiceTemplate> getTemplates() {
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
        if (this.ftpClient.changeWorkingDirectory(path)) {
            this.ftpClient.changeWorkingDirectory(super.baseDirectory);
            return;
        }

        for (String pathSegment : path.split("/")) {
            if (!this.ftpClient.changeWorkingDirectory(pathSegment)) {
                this.ftpClient.makeDirectory(pathSegment);
                this.ftpClient.changeWorkingDirectory(pathSegment);
            }
        }

        this.ftpClient.changeWorkingDirectory(super.baseDirectory);
    }

}