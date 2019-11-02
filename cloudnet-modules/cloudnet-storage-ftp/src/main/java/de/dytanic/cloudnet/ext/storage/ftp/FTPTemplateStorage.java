package de.dytanic.cloudnet.ext.storage.ftp;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FTPTemplateStorage implements ITemplateStorage {

    private static final LogLevel LOG_LEVEL = new LogLevel("ftp/ftps", "FTP/FTPS", 1, true);

    private final String name;
    private final JsonDocument document;

    private final FTPClient ftpClient;

    public FTPTemplateStorage(String name, JsonDocument document) {
        this.name = name;
        this.document = document;

        this.ftpClient = document.getBoolean("ssl") ? new FTPSClient() : new FTPClient();
    }

    @Override
    public boolean deploy(byte[] zipInput, ServiceTemplate target) {
        Validate.checkNotNull(zipInput);
        Validate.checkNotNull(target);

        if (this.has(target)) {
            delete(target);
        }

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipInput);
             ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream, StandardCharsets.UTF_8)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                makeDirectories(target.getTemplatePath());

                this.deploy0(zipInputStream, zipEntry, target.getTemplatePath());
                zipInputStream.closeEntry();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private void deploy0(ZipInputStream zipInputStream, ZipEntry zipEntry, String targetDirectory) throws IOException {
        this.checkConnection();

        if (zipEntry.isDirectory()) {
            this.makeDirectories(targetDirectory + "/" + zipEntry.getName());
        } else {
            this.ftpClient.storeFile(targetDirectory + "/" + zipEntry.getName(), zipInputStream);
        }
    }

    @Override
    public boolean deploy(File directory, ServiceTemplate target) {
        Validate.checkNotNull(directory);
        Validate.checkNotNull(target);

        if (directory.exists()) {
            File[] files = directory.listFiles();

            boolean value = false;

            if (files != null) {
                value = this.deploy(files, target);
            }

            return value;
        }

        return false;
    }

    @Override
    public boolean deploy(Path[] paths, ServiceTemplate target) {
        Validate.checkNotNull(paths);
        Validate.checkNotNull(target);

        return this.deploy(Iterables.map(Arrays.asList(paths), Path::toFile).toArray(new File[0]), target);
    }

    @Override
    public boolean deploy(File[] files, ServiceTemplate target) {
        Validate.checkNotNull(files);
        Validate.checkNotNull(target);

        if (this.has(target)) {
            this.delete(target);
        }

        try {
            this.makeDirectories(target.getTemplatePath());

            for (File file : files) {
                if (file != null) {
                    this.deploy0(file, target.getTemplatePath());
                }
            }

            return true;

        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

    }

    private void deploy0(File file, String targetDirectory) throws IOException {
        this.checkConnection();

        if (file.isDirectory()) {
            makeDirectories(targetDirectory + "/" + file.getName());

            File[] files = file.listFiles();

            if (files != null) {
                for (File entry : files) {
                    deploy0(entry, targetDirectory + "/" + file.getName());
                }
            }

        } else {
            try (InputStream inputStream = new FileInputStream(file)) {
                this.ftpClient.storeFile(targetDirectory + "/" + file.getName(), inputStream);
            }
        }
    }

    @Override
    public boolean copy(ServiceTemplate template, File directory) {
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
                    this.copy0(template.getTemplatePath() + "/" + file.getName(), file, directory);
                }
            }
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    private void copy0(String filePath, FTPFile file, File directory) throws IOException {
        this.checkConnection();

        directory.mkdirs();

        if (file.isDirectory()) {

            FTPFile[] files = this.ftpClient.listFiles(filePath);

            if (files != null) {
                for (FTPFile entry : files) {
                    this.copy0(filePath + "/" + entry.getName(), entry, new File(directory, file.getName()));
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
    public boolean copy(ServiceTemplate template, Path directory) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directory);

        return this.copy(template, directory.toFile());
    }

    @Override
    public boolean copy(ServiceTemplate template, File[] directories) {
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
    public boolean copy(ServiceTemplate template, Path[] directories) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directories);

        return this.copy(template, Iterables.map(Arrays.asList(directories), Path::toFile).toArray(new File[0]));
    }

    @Override
    public byte[] toZipByteArray(ServiceTemplate template) {
        if (!this.has(template)) {
            return FileUtils.emptyZipByteArray();
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {

            this.toByteArray0(zipOutputStream, template.getTemplatePath(), "");

            return byteArrayOutputStream.toByteArray();

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return FileUtils.emptyZipByteArray();
    }

    private void toByteArray0(ZipOutputStream zipOutputStream, String baseDirectory, String relativeDirectory) throws IOException {
        this.checkConnection();

        FTPFile[] files = this.ftpClient.listFiles(baseDirectory);

        if (files != null) {
            for (FTPFile file : files) {
                if (file != null) {
                    if (file.isDirectory()) {
                        toByteArray0(zipOutputStream, baseDirectory + "/" + file.getName(),
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
    public boolean delete(ServiceTemplate template) {
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
    public boolean create(ServiceTemplate template) {
        Validate.checkNotNull(template);

        try {
            this.makeDirectories(template.getTemplatePath());
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private void deleteDir(String path) throws IOException {
        this.checkConnection();

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
    public boolean has(ServiceTemplate template) {
        Validate.checkNotNull(template);
        this.checkConnection();

        try {
            return this.ftpClient.listFiles(template.getTemplatePath()).length > 0;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public OutputStream appendOutputStream(ServiceTemplate template, String path) throws IOException {
        this.checkConnection();

        return this.ftpClient.appendFileStream(template.getTemplatePath() + "/" + path);
    }

    @Override
    public OutputStream newOutputStream(ServiceTemplate template, String path) throws IOException {
        this.checkConnection();

        return this.ftpClient.storeFileStream(template.getTemplatePath() + "/" + path);
    }

    @Override
    public boolean createFile(ServiceTemplate template, String path) throws IOException {
        this.checkConnection();

        return this.ftpClient.storeFile(template.getTemplatePath() + "/" + path, new ByteArrayInputStream(new byte[0]));
    }

    @Override
    public boolean createDirectory(ServiceTemplate template, String path) throws IOException {
        this.checkConnection();

        this.makeDirectories(template.getTemplatePath() + "/" + path);
        return true;
    }

    @Override
    public boolean hasFile(ServiceTemplate template, String path) throws IOException {
        this.checkConnection();

        FTPFile file = this.ftpClient.mlistFile(template.getTemplatePath() + "/" + path);
        return file != null;
    }

    @Override
    public boolean deleteFile(ServiceTemplate template, String path) throws IOException {
        this.checkConnection();

        return this.ftpClient.deleteFile(template.getTemplatePath() + "/" + path);
    }

    @Override
    public String[] listFiles(ServiceTemplate template, String dir) throws IOException {
        this.checkConnection();

        return this.ftpClient.listNames(dir);
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        this.checkConnection();

        Collection<ServiceTemplate> templates = Iterables.newArrayList();

        try {
            FTPFile[] files = this.ftpClient.listFiles();

            if (files != null) {
                for (FTPFile entry : files) {
                    if (entry.isDirectory()) {
                        FTPFile[] subPathEntries = this.ftpClient.listFiles(entry.getName());

                        if (subPathEntries != null) {
                            for (FTPFile subEntry : subPathEntries) {
                                if (subEntry.isDirectory()) {
                                    templates.add(new ServiceTemplate(entry.getName(), subEntry.getName(), document.getString("storage")));
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

    @Override
    public void close() throws IOException {
        CloudNetDriver.getInstance().getLogger().log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-disconnect"));
        this.ftpClient.disconnect();
    }


    private void makeDirectories(String pathname) throws IOException {
        this.checkConnection();

        boolean dirExists = true;

        String[] directories = pathname.startsWith("/") ? pathname.split("/") : ("/" + pathname).split("/");
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                if (dirExists) {
                    dirExists = this.ftpClient.changeWorkingDirectory(dir);
                }

                if (!dirExists) {
                    if (!this.ftpClient.makeDirectory(dir)) {
                        CloudNetDriver.getInstance().getLogger().log(LogLevel.WARNING,
                                "failed to make directory " + dir + " " + this.ftpClient.getReplyString());
                        return;
                    }

                    if (!this.ftpClient.changeWorkingDirectory(dir)) {
                        CloudNetDriver.getInstance().getLogger().log(LogLevel.WARNING,
                                "failed to change this directory " + dir + " " + this.ftpClient.getReplyString());
                        return;
                    }
                }
            }
        }

        this.resetWorkingDirectory();
    }

    private void resetWorkingDirectory() throws IOException {
        this.ftpClient.changeWorkingDirectory(this.document.getString("baseDirectory"));
    }

    private void checkConnection() {
        if (!this.ftpClient.isConnected() || !this.ftpClient.isAvailable()) {
            this.connectToFTPServer();
        }
    }

    private void connectToFTPServer() {
        ILogger logger = CloudNetDriver.getInstance().getLogger();

        try {
            HostAndPort address = this.document.get("address", HostAndPort.class);

            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect")
                    .replace("%host%", address.getHost())
                    .replace("%port%", String.valueOf(address.getPort()))
            );
            this.ftpClient.connect(address.getHost(), address.getPort());
            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect-success")
                    .replace("%host%", address.getHost())
                    .replace("%port%", String.valueOf(address.getPort()))
            );

            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-login")
                    .replace("%user%", this.document.getString("username"))
            );
            this.ftpClient.login(this.document.getString("username"), this.document.getString("password"));
            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-login-success")
                    .replace("%user%", this.document.getString("username"))
            );

            this.ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            this.ftpClient.setAutodetectUTF8(true);
            this.ftpClient.setKeepAlive(true);
            this.ftpClient.setBufferSize(this.document.getInt("bufferSize"));
            this.ftpClient.changeWorkingDirectory(this.document.getString("baseDirectory"));

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public JsonDocument getDocument() {
        return this.document;
    }

    @Override
    public String getName() {
        return this.name;
    }

}