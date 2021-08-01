/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.template.install;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.io.HttpConnectionProvider;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.install.run.InstallInformation;
import de.dytanic.cloudnet.template.install.run.step.InstallStep;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ServiceVersionProvider {

  private static final Path VERSION_CACHE_PATH = Paths.get(
    System.getProperty("cloudnet.versioncache.path", "local/versioncache"));

  private static final int VERSIONS_FILE_VERSION = 1;
  private static final Type COLLECTION_SERVICE_VERSION_TYPE = TypeToken
    .getParameterized(Collection.class, ServiceVersionType.class).getType();

  private final Map<String, ServiceVersionType> serviceVersionTypes = new HashMap<>();

  public boolean loadServiceVersionTypes(String url) throws IOException {
    this.serviceVersionTypes.clear();

    HttpURLConnection connection = HttpConnectionProvider.provideConnection(url);
    connection.setUseCaches(false);
    connection.connect();

    try (InputStream inputStream = connection.getInputStream()) {
      return this.loadVersionsFromInputStream(inputStream);
    }
  }

  public void loadDefaultVersionTypes() {
    this.loadVersionsFromInputStream(this.getClass().getClassLoader().getResourceAsStream("files/versions.json"));
  }

  private boolean loadVersionsFromInputStream(InputStream inputStream) {
    if (inputStream == null) {
      return false;
    }

    JsonDocument document = JsonDocument.newDocument(inputStream);
    int fileVersion = document.getInt("fileVersion", -1);

    if (VERSIONS_FILE_VERSION == fileVersion && document.contains("versions")) {
      Collection<ServiceVersionType> versions = document.get("versions", COLLECTION_SERVICE_VERSION_TYPE);
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

  public Optional<ServiceVersionType> getServiceVersionType(@NotNull String name) {
    return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
  }

  public boolean installServiceVersion(ServiceVersionType type, ServiceVersion version, ServiceTemplate template) {
    return this.installServiceVersion(type, version, template.storage().getWrappedStorage(), template);
  }

  public boolean installServiceVersion(String executable, ServiceVersionType type,
    ServiceVersion version, ServiceTemplate template) {
    return this.installServiceVersion(
      new InstallInformation(executable, version, template.storage().getWrappedStorage(), template, type), false);
  }

  public boolean installServiceVersion(ServiceVersionType type, ServiceVersion version, ServiceTemplate template,
    boolean force) {
    return this.installServiceVersion(type, version, template.storage().getWrappedStorage(), template, force);
  }

  public boolean installServiceVersion(String executable, ServiceVersionType type, ServiceVersion version,
    ServiceTemplate template, boolean force) {
    return this.installServiceVersion(
      new InstallInformation(executable, version, template.storage().getWrappedStorage(), template, type), force);
  }

  public boolean installServiceVersion(ServiceVersionType type, ServiceVersion version, TemplateStorage storage,
    ServiceTemplate template) {
    return this.installServiceVersion(type, version, storage, template, false);
  }

  public boolean installServiceVersion(
    ServiceVersionType serviceVersionType, ServiceVersion serviceVersion,
    TemplateStorage storage, ServiceTemplate serviceTemplate, boolean forceInstall
  ) {
    return this.installServiceVersion(
      new InstallInformation(serviceVersion, storage, serviceTemplate, serviceVersionType), forceInstall);
  }

  public boolean installServiceVersion(InstallInformation information, boolean force) {
    String fullVersionIdentifier =
      information.getServiceVersionType().getName() + "-" + information.getServiceVersion().getName();

    if (!force
      && !information.getInstallerExecutable().isPresent()
      && !information.getServiceVersionType().canInstall(information.getServiceVersion())
    ) {
      throw new IllegalArgumentException(String.format(
        "Cannot run %s on %s",
        fullVersionIdentifier,
        JavaVersion.getRuntimeVersion().getName()
      ));
    }

    if (information.getServiceVersion().isDeprecated()) {
      CloudNet.getInstance().getLogger().warning(LanguageManager.getMessage("versions-installer-deprecated-version")
        .replace("%version%", fullVersionIdentifier)
      );
    }

    if (!information.getTemplateStorage().has(information.getServiceTemplate())) {
      information.getTemplateStorage().create(information.getServiceTemplate());
    }

    try {
      // delete all old application files to prevent that they are used to start the service
      for (FileInfo file : information.getTemplateStorage().listFiles(information.getServiceTemplate(), "", false)) {
        if (file != null) {
          for (ServiceEnvironment environment : ServiceEnvironment.VALUES) {
            if (file.getName().toLowerCase().contains(environment.getName()) && file.getName().endsWith(".jar")) {
              information.getTemplateStorage().deleteFile(information.getServiceTemplate(), file.getPath());
            }
          }
        }
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    Path workingDirectory = FileUtils.createTempFile();
    Path cacheDir = VERSION_CACHE_PATH.resolve(fullVersionIdentifier);

    try {
      if (information.getServiceVersion().isCacheFiles() && Files.exists(cacheDir)) {
        InstallStep.DEPLOY.execute(information, cacheDir, Files.walk(cacheDir).collect(Collectors.toSet()));
      } else {
        Files.createDirectories(workingDirectory);

        List<InstallStep> installSteps = new ArrayList<>(information.getServiceVersionType().getInstallSteps());
        installSteps.add(InstallStep.DEPLOY);

        Set<Path> lastStepResult = new HashSet<>();
        for (InstallStep installStep : installSteps) {
          lastStepResult = installStep.execute(information, workingDirectory, lastStepResult);
        }

        if (information.getServiceVersion().isCacheFiles()) {
          for (Path path : lastStepResult) {
            Path targetPath = cacheDir.resolve(workingDirectory.relativize(path));
            Files.createDirectories(targetPath.getParent());

            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }

      for (Map.Entry<String, String> entry : information.getServiceVersion().getAdditionalDownloads().entrySet()) {
        String path = entry.getKey();
        String url = entry.getValue();

        try (InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), url);
          OutputStream out = information.getTemplateStorage().newOutputStream(information.getServiceTemplate(), path)) {
          FileUtils.copy(inputStream, out);
        }
      }

      return true;
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      FileUtils.delete(workingDirectory);
    }

    return false;
  }

  public Map<String, ServiceVersionType> getServiceVersionTypes() {
    return this.serviceVersionTypes;
  }
}
