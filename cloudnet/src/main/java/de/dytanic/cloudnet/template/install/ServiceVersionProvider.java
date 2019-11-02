package de.dytanic.cloudnet.template.install;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.installer.ServiceVersionInstaller;
import de.dytanic.cloudnet.template.install.installer.SimpleDownloadingServiceVersionInstaller;
import de.dytanic.cloudnet.template.install.installer.processing.ProcessingServiceVersionInstaller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServiceVersionProvider {

    private final Map<ServiceVersionType.InstallerType, ServiceVersionInstaller> installers = new HashMap<>();

    private final Map<String, ServiceVersionType> serviceVersionTypes = new HashMap<>();

    public ServiceVersionProvider() {
        this.installers.put(ServiceVersionType.InstallerType.DOWNLOAD, new SimpleDownloadingServiceVersionInstaller());
        this.installers.put(ServiceVersionType.InstallerType.BUILD, new ProcessingServiceVersionInstaller());
    }

    public boolean loadServiceVersionTypes(String url) throws IOException {
        this.serviceVersionTypes.clear();

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        connection.connect();

        boolean success = this.loadVersionsFromInputStream(connection.getInputStream());

        connection.disconnect();

        return success;
    }

    public void loadDefaultVersionTypes() {
        this.loadVersionsFromInputStream(this.getClass().getClassLoader().getResourceAsStream("files/versions.json"));
    }

    private boolean loadVersionsFromInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return false;
        }

        JsonDocument document = new JsonDocument().read(inputStream);

        if (document.contains("versions")) {
            Collection<ServiceVersionType> versions = document.get("versions", TypeToken.getParameterized(Collection.class, ServiceVersionType.class).getType());

            for (ServiceVersionType serviceVersionType : versions) {
                this.registerServiceVersionType(serviceVersionType);
            }

            return true;
        }
        return false;
    }


    public void registerServiceVersionType(ServiceVersionType serviceVersionType) {
        this.serviceVersionTypes.put(serviceVersionType.getName().toLowerCase(), serviceVersionType);
    }

    public Optional<ServiceVersionType> getServiceVersionType(String name) {
        return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
    }


    public boolean installServiceVersion(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion, ITemplateStorage storage, ServiceTemplate serviceTemplate) {
        if (!serviceVersionType.getInstallerType().canInstall(serviceVersion)) {
            throw new IllegalStateException("Cannot run " + serviceVersionType.getName() + "-" + serviceVersion.getName() + "#" + serviceVersionType.getInstallerType() + " on " + JavaVersion.getRuntimeVersion().getName());
        }

        ServiceVersionInstaller installer = this.installers.get(serviceVersionType.getInstallerType());

        if (installer == null) {
            throw new IllegalArgumentException("Installer for type " + serviceVersionType.getInstallerType() + " not found");
        }

        if (!storage.has(serviceTemplate)) {
            storage.create(serviceTemplate);
        }

        String fileName = serviceVersionType.getTargetEnvironment().getName() + ".jar";
        Path workingDirectory = Paths.get(System.getProperty("cloudnet.tempDir.build", "temp/build"), UUID.randomUUID().toString());

        Path versionTypeCacheDir = Paths.get(
                System.getProperty("cloudnet.versioncache.path", "local/versioncache"),
                serviceVersionType.getName()
        );
        Path versionCacheFile = Paths.get(versionTypeCacheDir.toString(), serviceVersion.getName() + ".jar");

        try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, fileName)) {

            if (Files.exists(versionCacheFile)) {
                Files.copy(versionCacheFile, outputStream);
            } else {
                Files.createDirectories(workingDirectory);

                if (!serviceVersion.isLatest()) {
                    Files.createDirectories(versionTypeCacheDir);

                    try (OutputStream cacheFileStream = new FileOutputStream(versionCacheFile.toFile())) {
                        installer.install(serviceVersion, workingDirectory, outputStream, cacheFileStream);
                    }

                } else {
                    installer.install(serviceVersion, workingDirectory, outputStream);
                }
            }

        } catch (Exception exception) {
            FileUtils.delete(versionCacheFile.toFile());
            return false;
        } finally {
            FileUtils.delete(workingDirectory.toFile());
        }

        try {
            //delete all old application files if they exist to prevent that they are used to start the server
            for (String file : storage.listFiles(serviceTemplate)) {
                if (file.equals(fileName)) {
                    continue;
                }
                for (ServiceEnvironment environment : ServiceEnvironment.values()) {
                    if (file.equalsIgnoreCase(environment.getName() + ".jar")) {
                        storage.deleteFile(serviceTemplate, file);
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return true;
    }

    public Map<String, ServiceVersionType> getServiceVersionTypes() {
        return this.serviceVersionTypes;
    }

}
