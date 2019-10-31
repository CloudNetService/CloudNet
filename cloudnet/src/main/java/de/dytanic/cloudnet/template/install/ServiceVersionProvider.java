package de.dytanic.cloudnet.template.install;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.installer.ProcessingServiceVersionInstaller;
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

    private final Map<String, ServiceVersionInstaller> installers = new HashMap<>();

    {
        this.installers.put("download", new SimpleDownloadingServiceVersionInstaller());
        this.installers.put("processing", new ProcessingServiceVersionInstaller());
    }

    private Map<String, ServiceVersionType> installableVersions = new HashMap<>();

    public boolean loadVersions(String url) {
        this.installableVersions.clear();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

            connection.connect();

            boolean success = false;

            try (InputStream inputStream = connection.getInputStream()) {
                JsonDocument document = new JsonDocument().read(inputStream);

                Collection<ServiceVersionType> versions = document.get("versions", TypeToken.getParameterized(Collection.class, ServiceVersionType.class).getType());

                if (versions != null) {
                    for (ServiceVersionType version : versions) {
                        this.registerVersion(version);
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
        for (ServiceVersionType version : this.getDefaultVersionTypes()) {
            this.registerVersion(version);
        }
        return false;
    }

    public void registerVersion(ServiceVersionType versionType) {
        this.installableVersions.put(versionType.getName(), versionType);
    }

    public Map<String, ServiceVersionType> getInstallableVersions() {
        return this.installableVersions;
    }

    public Collection<ServiceVersionType> getDefaultVersionTypes() {
        return Arrays.asList(
                new ServiceVersionType(
                        "paperspigot",
                        "paper",
                        "processing",
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
                        "spigot",
                        "download",
                        Arrays.asList(
                                new ServiceVersion(
                                        "1.13.2",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.13.2.jar"
                                )
                        )
                )
        );
    }

    public void installServiceVersion(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion, ITemplateStorage storage, ServiceTemplate serviceTemplate) {
        ServiceVersionInstaller installer = this.installers.get(serviceVersionType.getInstaller());
        if (installer == null) {
            throw new IllegalArgumentException("Installer " + serviceVersionType.getInstaller() + " not found");
        }

        Path workingDirectory = Paths.get("temp/build/" + UUID.randomUUID());
        try {
            Files.createDirectories(workingDirectory.getParent());

            try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, serviceVersionType.getName())) {
                installer.install(serviceVersion, workingDirectory, outputStream);
            }

            FileUtils.delete(workingDirectory.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
