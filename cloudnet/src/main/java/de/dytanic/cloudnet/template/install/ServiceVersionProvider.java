package de.dytanic.cloudnet.template.install;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.run.InstallInformation;
import de.dytanic.cloudnet.template.install.run.step.InstallStep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceVersionProvider {

    private static final int VERSIONS_FILE_VERSION = 1;

    private final Map<String, ServiceVersionType> serviceVersionTypes = new HashMap<>();

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

        int fileVersion = document.getInt("fileVersion", -1);

        if (VERSIONS_FILE_VERSION == fileVersion && document.contains("versions")) {
            Collection<ServiceVersionType> versions = document.get("versions", TypeToken.getParameterized(Collection.class, ServiceVersionType.class).getType());

            for (ServiceVersionType serviceVersionType : versions) {
                this.registerServiceVersionType(serviceVersionType);
            }

            return true;
        }
        return false;
    }

    public void interruptInstallSteps() {
        for (InstallStep installStep : InstallStep.values()) {
            installStep.interrupt();
        }
    }

    public void registerServiceVersionType(ServiceVersionType serviceVersionType) {
        this.serviceVersionTypes.put(serviceVersionType.getName().toLowerCase(), serviceVersionType);
    }

    public Optional<ServiceVersionType> getServiceVersionType(String name) {
        return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
    }


    public boolean installServiceVersion(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion, ServiceTemplate serviceTemplate) {
        ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, serviceTemplate.getStorage());
        if (storage == null) {
            throw new IllegalArgumentException("Storage " + serviceTemplate.getStorage() + " not found");
        }
        return this.installServiceVersion(serviceVersionType, serviceVersion, storage, serviceTemplate);
    }

    public boolean installServiceVersion(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion, ITemplateStorage storage, ServiceTemplate serviceTemplate) {
        if (!serviceVersionType.canInstall(serviceVersion)) {
            throw new IllegalStateException("Cannot run " + serviceVersionType.getName() + "-" + serviceVersion.getName() + " on " + JavaVersion.getRuntimeVersion().getName());
        }

        if (serviceVersion.isDeprecated()) {
            CloudNet.getInstance().getLogger().warning(LanguageManager.getMessage("versions-installer-deprecated-version")
                    .replace("%version%", serviceVersionType.getName() + "-" + serviceVersion.getName())
            );
        }

        if (!storage.has(serviceTemplate)) {
            storage.create(serviceTemplate);
        }

        try {
            //delete all old application files if they exist to prevent that they are used to start the server
            for (String file : storage.listFiles(serviceTemplate)) {
                for (ServiceEnvironment environment : ServiceEnvironment.values()) {
                    if (file.toLowerCase().contains(environment.getName()) && file.endsWith(".jar")) {
                        storage.deleteFile(serviceTemplate, file);
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        Path workingDirectory = Paths.get(System.getProperty("cloudnet.tempDir.build", "temp/build"), UUID.randomUUID().toString());

        Path versionCacheDirectory = Paths.get(System.getProperty("cloudnet.versioncache.path", "local/versioncache"),
                serviceVersionType.getName() + "-" + serviceVersion.getName());

        InstallInformation installInformation = new InstallInformation(serviceVersionType, serviceVersion, storage, serviceTemplate);

        try {
            if (serviceVersion.isCacheFiles() && Files.exists(versionCacheDirectory)) {
                InstallStep.DEPLOY.execute(installInformation, versionCacheDirectory, Files.walk(versionCacheDirectory).collect(Collectors.toSet()));
            } else {
                Files.createDirectories(workingDirectory);

                List<InstallStep> installSteps = new ArrayList<>();

                installSteps.add(InstallStep.DOWNLOAD);
                installSteps.addAll(serviceVersionType.getInstallSteps());
                installSteps.add(InstallStep.DEPLOY);

                Set<Path> lastStepResult = new HashSet<>();

                for (InstallStep installStep : installSteps) {
                    lastStepResult = installStep.execute(installInformation, workingDirectory, lastStepResult);
                }

                if (serviceVersion.isCacheFiles()) {
                    for (Path path : lastStepResult) {
                        Path targetPath = versionCacheDirectory.resolve(workingDirectory.relativize(path));
                        Files.createDirectories(targetPath.getParent());

                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            for (Map.Entry<String, String> downloadEntry : serviceVersion.getAdditionalDownloads().entrySet()) {
                String path = downloadEntry.getKey();
                String url = downloadEntry.getValue();

                try (InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), new URL(url));
                     OutputStream outputStream = storage.newOutputStream(serviceTemplate, path)) {
                    FileUtils.copy(inputStream, outputStream);
                }
            }

            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            FileUtils.delete(workingDirectory.toFile());
        }

        return false;
    }

    public Map<String, ServiceVersionType> getServiceVersionTypes() {
        return this.serviceVersionTypes;
    }

}
