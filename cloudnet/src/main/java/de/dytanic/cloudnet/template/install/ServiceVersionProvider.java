package de.dytanic.cloudnet.template.install;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.installer.ProcessingServiceVersionInstaller;
import de.dytanic.cloudnet.template.install.installer.ServiceVersionInstaller;
import de.dytanic.cloudnet.template.install.installer.SimpleDownloadingServiceVersionInstaller;

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

    public boolean loadServiceVersionTypes(String url) {
        this.serviceVersionTypes.clear();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

            connection.connect();

            boolean success = false;

            try (InputStream inputStream = connection.getInputStream()) {
                JsonDocument document = new JsonDocument().read(inputStream);

                if (document.contains("versions")) {
                    Collection<ServiceVersionType> versions = document.get("versions", TypeToken.getParameterized(Collection.class, ServiceVersionType.class).getType());

                    for (ServiceVersionType serviceVersionType : versions) {
                        this.registerServiceVersionType(serviceVersionType);
                    }

                    success = true;
                }
            }

            connection.disconnect();
            if (success) {
                return true;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        for (ServiceVersionType serviceVersionType : this.createDefaultVersionTypes()) {
            this.registerServiceVersionType(serviceVersionType);
        }

        return false;
    }

    public void registerServiceVersionType(ServiceVersionType serviceVersionType) {
        this.serviceVersionTypes.put(serviceVersionType.getName().toLowerCase(), serviceVersionType);
    }

    public Optional<ServiceVersionType> getServiceVersionType(String name) {
        return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
    }

    public void installServiceVersion(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion, ITemplateStorage storage, ServiceTemplate serviceTemplate) {
        ServiceVersionInstaller installer = this.installers.get(serviceVersionType.getInstallerType());

        if (installer == null) {
            throw new IllegalArgumentException("Installer for type " + serviceVersionType.getInstallerType() + " not found");
        }

        Path workingDirectory = Paths.get("temp/build/" + UUID.randomUUID());
        try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, serviceVersionType.getTargetEnvironment().getName() + ".jar")) {
            Files.createDirectories(workingDirectory.getParent());

            installer.install(serviceVersion, workingDirectory, outputStream);

            FileUtils.delete(workingDirectory.toFile());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public Collection<ServiceVersionType> createDefaultVersionTypes() {
        return Arrays.asList(
                new ServiceVersionType(
                        "paperspigot",
                        ServiceEnvironment.MINECRAFT_SERVER_PAPER_SPIGOT,
                        ServiceVersionType.InstallerType.BUILD,
                        Arrays.asList(
                                new ServiceVersion(
                                        "1.14.4",
                                        "https://papermc.io/ci/job/Paper-1.14/lastSuccessfulBuild/artifact/paperclip.jar"
                                ),
                                new ServiceVersion(
                                        "1.13.2",
                                        "https://papermc.io/ci/job/Paper-1.13/lastSuccessfulBuild/artifact/paperclip.jar"
                                )
                        )
                ),
                new ServiceVersionType(
                        "spigot",
                        ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
                        ServiceVersionType.InstallerType.DOWNLOAD,
                        Arrays.asList(
                                new ServiceVersion(
                                        "1.13.2",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.13.2.jar"
                                )
                        )
                )
        );
    }

    public Map<String, ServiceVersionType> getServiceVersionTypes() {
        return serviceVersionTypes;
    }

    /*

        [
          {
            "name": "paperspigot",
            "installer": "processing",
            "fileName": "paper",
            "versions": [
              {
                "name": "1.14.4",
                "url": "PaperMC CI Link",
                "properties": {
                  "command": "java -jar download.jar",
                  "copy": "temp/patched.*\\.jar"
                }
              }
            ]
          },
          {
            "name": "spigot",
            "installer": "download",
            "fileName": "spigot",
            "versions": [
              {
                "name": "1.14.4",
                "url": "getbukkit link"
              },
              {
                "name": "1.13.2",
                "url": "getbukkit link"
              }
            ]
          }
        ]

     */

}
