package de.dytanic.cloudnet.ext.storage.ftp;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.concurrent.IVoidThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
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

    private final JsonDocument document;

    public FTPTemplateStorage(JsonDocument document) {
        this.document = document;
    }

    @Override
    public boolean deploy(byte[] zipInput, ServiceTemplate target) {
        Validate.checkNotNull(zipInput);
        Validate.checkNotNull(target);

        if (has(target)) {
            delete(target);
        }

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipInput);
             ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream, StandardCharsets.UTF_8)) {
            return handleWithFTPClient(ftpClient -> {

                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    makeDirectories(ftpClient, target.getTemplatePath());

                    deploy0(ftpClient, zipInputStream, zipEntry, target.getTemplatePath());
                    zipInputStream.closeEntry();
                }

                return null;
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void deploy0(FTPClient ftpClient, ZipInputStream zipInputStream, ZipEntry zipEntry, String targetDirectory) throws Exception {
        if (zipEntry.isDirectory()) {
            makeDirectories(ftpClient, targetDirectory + "/" + zipEntry.getName());
        } else {
            ftpClient.storeFile(targetDirectory + "/" + zipEntry.getName(), zipInputStream);
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

        if (has(target)) {
            delete(target);
        }

        return this.handleWithFTPClient(ftpClient -> {
            makeDirectories(ftpClient, target.getTemplatePath());

            for (File file : files) {
                if (file != null) {
                    deploy0(ftpClient, file, target.getTemplatePath());
                }
            }

            return null;
        });
    }

    private void deploy0(FTPClient ftpClient, File file, String targetDirectory) throws Exception {
        if (file.isDirectory()) {
            makeDirectories(ftpClient, targetDirectory + "/" + file.getName());

            File[] files = file.listFiles();

            if (files != null) {
                for (File entry : files) {
                    deploy0(ftpClient, entry, targetDirectory + "/" + file.getName());
                }
            }

        } else {
            try (InputStream inputStream = new FileInputStream(file)) {
                ftpClient.storeFile(targetDirectory + "/" + file.getName(), inputStream);
            }
        }
    }

    @Override
    public boolean copy(ServiceTemplate template, File directory) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directory);

        if (!has(template)) {
            return false;
        }

        directory.mkdirs();

        return handleWithFTPClient(ftpClient -> {
            FTPFile[] files = ftpClient.listFiles(template.getTemplatePath());

            if (files != null) {
                for (FTPFile file : files) {
                    copy0(ftpClient, template.getTemplatePath() + "/" + file.getName(), file, directory);
                }
            }

            return null;
        });
    }

    private void copy0(FTPClient ftpClient, String filePath, FTPFile file, File directory) throws Exception {
        directory.mkdirs();

        if (file.isDirectory()) {

            FTPFile[] files = ftpClient.listFiles(filePath);

            if (files != null) {
                for (FTPFile entry : files) {
                    copy0(ftpClient, filePath + "/" + entry.getName(), entry, new File(directory, file.getName()));
                }
            }

        } else if (file.isFile()) {
            File entry = new File(directory, file.getName());
            entry.getParentFile().mkdirs();

            if (!entry.exists()) {
                entry.createNewFile();

                try (OutputStream outputStream = new FileOutputStream(entry)) {
                    ftpClient.retrieveFile(filePath, outputStream);
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
        if (!has(template)) {
            return FileUtils.emptyZipByteArray();
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {
            handleWithFTPClient(ftpClient -> {
                toByteArray0(ftpClient, zipOutputStream, template.getTemplatePath(), "");
                return null;
            });

            return byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return FileUtils.emptyZipByteArray();
    }

    private void toByteArray0(FTPClient ftpClient, ZipOutputStream zipOutputStream, String baseDirectory, String relativeDirectory) throws Exception {
        FTPFile[] files = ftpClient.listFiles(baseDirectory);

        if (files != null) {
            for (FTPFile file : files) {
                if (file != null) {
                    if (file.isDirectory()) {
                        toByteArray0(ftpClient, zipOutputStream, baseDirectory + "/" + file.getName(),
                                relativeDirectory + (relativeDirectory.isEmpty() ? "" : "/") + file.getName());
                    } else if (file.isFile()) {
                        zipOutputStream.putNextEntry(new ZipEntry(relativeDirectory + (relativeDirectory.isEmpty() ? "" : "/") + file.getName()));
                        ftpClient.retrieveFile(baseDirectory + "/" + file.getName(), zipOutputStream);
                        zipOutputStream.closeEntry();
                    }
                }
            }
        }
    }

    @Override
    public boolean delete(ServiceTemplate template) {
        Validate.checkNotNull(template);

        return handleWithFTPClient(ftpClient -> {
            FTPFile file = ftpClient.mlistFile(template.getTemplatePath());
            delete0(ftpClient, template, file, "");
            return null;
        });
    }

    @Override
    public boolean create(ServiceTemplate template) {
        return handleWithFTPClient(ftpClient -> {
            makeDirectories(ftpClient, template.getTemplatePath());
            return null;
        });
    }

    private void delete0(FTPClient ftpClient, ServiceTemplate template, FTPFile file, String path) throws Exception {
        if (file.isDirectory()) {
            FTPFile[] files = path.isEmpty() ? ftpClient.listFiles() : ftpClient.listFiles(template.getTemplatePath());

            if (files != null) {
                for (FTPFile entry : files) {
                    if (entry.isDirectory()) {
                        delete0(ftpClient, template, entry, path + (path.isEmpty() ? "" : "/") + entry.getName());
                    } else {
                        ftpClient.deleteFile(path + entry.getName());
                    }
                }
            }
        }

        ftpClient.removeDirectory(template.getTemplatePath());
    }

    @Override
    public boolean has(ServiceTemplate template) {
        Validate.checkNotNull(template);

        Value<Boolean> result = new Value<>(false);

        handleWithFTPClient(ftpClient -> {
            result.setValue(ftpClient.listFiles(template.getTemplatePath()).length > 0);
            return null;
        });

        return result.getValue();
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        Collection<ServiceTemplate> templates = Iterables.newArrayList();

        handleWithFTPClient(ftpClient -> {
            FTPFile[] files = ftpClient.listFiles();

            if (files != null) {
                for (FTPFile entry : files) {
                    if (entry.isDirectory()) {
                        FTPFile[] subPathEntries = ftpClient.listFiles(entry.getName());

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

            return null;
        });

        return templates;
    }

    @Override
    public void close() {
    }


    private void makeDirectories(FTPClient ftpClient, String pathname) throws Exception {
        boolean dirExists = true;

        String[] directories = pathname.startsWith("/") ? pathname.split("/") : (ftpClient.printWorkingDirectory() + "/" + pathname).split("/");
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                if (dirExists) {
                    dirExists = ftpClient.changeWorkingDirectory(dir);
                }

                if (!dirExists) {
                    if (!ftpClient.makeDirectory(dir)) {
                        CloudNetDriver.getInstance().getLogger().log(LogLevel.WARNING,
                                "failed to make directory " + dir + " " + ftpClient.getReplyString());
                        return;
                    }

                    if (!ftpClient.changeWorkingDirectory(dir)) {
                        CloudNetDriver.getInstance().getLogger().log(LogLevel.WARNING,
                                "failed to change this directory " + dir + " " + ftpClient.getReplyString());
                        return;
                    }
                }
            }
        }

        resetWorkingDirectory(ftpClient);
    }

    private void resetWorkingDirectory(FTPClient ftpClient) throws Exception {
        ftpClient.changeWorkingDirectory(this.document.getString("baseDirectory"));
    }

    private boolean handleWithFTPClient(IVoidThrowableCallback<FTPClient> handler) {
        Validate.checkNotNull(handler);

        ILogger logger = CloudNetDriver.getInstance().getLogger();

        FTPClient ftpClient = document.getBoolean("ssl") ? new FTPSClient() : new FTPClient();
        try {
            HostAndPort address = this.document.get("address", HostAndPort.class);

            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect")
                    .replace("%host%", address.getHost())
                    .replace("%port%", String.valueOf(address.getPort()))
            );
            ftpClient.connect(address.getHost(), address.getPort());
            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect-success")
                    .replace("%host%", address.getHost())
                    .replace("%port%", String.valueOf(address.getPort()))
            );

            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-login")
                    .replace("%user%", this.document.getString("username"))
            );
            ftpClient.login(this.document.getString("username"), this.document.getString("password"));
            logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-login-success")
                    .replace("%user%", this.document.getString("username"))
            );

            ftpClient.setAutodetectUTF8(true);
            ftpClient.setBufferSize(this.document.getInt("bufferSize"));
            ftpClient.changeWorkingDirectory(this.document.getString("baseDirectory"));

            handler.call(ftpClient);

            return true;

        } catch (Throwable ex) {
            ex.printStackTrace();

            return false;

        } finally {
            try {
                logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-disconnect"));
                ftpClient.disconnect();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public JsonDocument getDocument() {
        return this.document;
    }
}