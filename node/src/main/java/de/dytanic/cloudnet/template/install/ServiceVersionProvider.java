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
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.animation.progressbar.ConsoleProgressWrappers;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.template.install.execute.InstallStep;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kong.unirest.Unirest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class ServiceVersionProvider {

  public static final String DEFAULT_FILE_URL = System.getProperty(
    "cloudnet.versions.url",
    "https://cloudnetservice.eu/cloudnet/versions.json");
  private static final Logger LOGGER = LogManager.getLogger(ServiceVersionProvider.class);

  private static final Path VERSION_CACHE_PATH = Paths.get(
    System.getProperty("cloudnet.versioncache.path", "local/versioncache"));

  private static final int VERSIONS_FILE_VERSION = 2;
  private static final Type COL_SV = TypeToken.getParameterized(Collection.class, ServiceVersionType.class).getType();

  private final Map<String, ServiceVersionType> serviceVersionTypes = new ConcurrentHashMap<>();

  public void loadServiceVersionTypesOrDefaults(@NotNull String url) {
    try {
      if (!this.loadServiceVersionTypes(url)) {
        this.loadDefaultVersionTypes();
      }
    } catch (IOException exception) {
      this.loadDefaultVersionTypes();
    }
  }

  public boolean loadServiceVersionTypes(@NotNull String url) throws IOException {
    this.serviceVersionTypes.clear();

    return Unirest
      .get(url)
      .asObject(rawResponse -> {
        if (rawResponse.getStatus() == 200) {
          return this.loadVersionsFromInputStream(rawResponse.getContent());
        } else {
          this.loadDefaultVersionTypes();
          return true;
        }
      })
      .getBody();
  }

  public void loadDefaultVersionTypes() {
    this.serviceVersionTypes.clear();
    this.loadVersionsFromInputStream(this.getClass().getClassLoader().getResourceAsStream("files/versions.json"));
  }

  private boolean loadVersionsFromInputStream(InputStream inputStream) {
    if (inputStream == null) {
      return false;
    }

    JsonDocument document = JsonDocument.newDocument(inputStream);
    int fileVersion = document.getInt("fileVersion", -1);

    if (VERSIONS_FILE_VERSION == fileVersion && document.contains("versions")) {
      Collection<ServiceVersionType> versions = document.get("versions", COL_SV);
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

  public void registerServiceVersionType(@NotNull ServiceVersionType serviceVersionType) {
    this.serviceVersionTypes.put(serviceVersionType.getName().toLowerCase(), serviceVersionType);
  }

  public @NotNull Optional<ServiceVersionType> getServiceVersionType(@NotNull String name) {
    return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
  }

  public boolean installServiceVersion(@NotNull InstallInformation information, boolean force) {
    String fullVersionIdentifier = String.format("%s-%s",
      information.getServiceVersionType().getName(),
      information.getServiceVersion().getName());

    if (!force
      && !information.getInstallerExecutable().isPresent()
      && !information.getServiceVersionType().canInstall(information.getServiceVersion())
    ) {
      throw new IllegalArgumentException(String.format(
        "Cannot run %s on %s",
        fullVersionIdentifier,
        JavaVersion.getRuntimeVersion().getName()));
    }

    if (information.getServiceVersion().isDeprecated()) {
      LOGGER.warning(I18n.trans("versions-installer-deprecated-version")
        .replace("%version%", fullVersionIdentifier));
    }

    try {
      // delete all old application files to prevent that they are used to start the service
      for (FileInfo file : information.getTemplateStorage().listFiles("", false)) {
        if (file != null) {
          for (ServiceEnvironment environment : ServiceEnvironment.VALUES) {
            if (file.getName().toLowerCase().contains(environment.getName()) && file.getName().endsWith(".jar")) {
              information.getTemplateStorage().deleteFile(file.getPath());
            }
          }
        }
      }
    } catch (IOException exception) {
      LOGGER.severe("Exception while deleting old application files", exception);
    }

    Path workingDirectory = FileUtils.createTempFile();
    Path cachedFilePath = VERSION_CACHE_PATH.resolve(fullVersionIdentifier);

    try {
      if (information.getServiceVersion().isCacheFiles() && Files.exists(cachedFilePath)) {
        InstallStep.DEPLOY.execute(information, cachedFilePath, Files.walk(cachedFilePath).collect(Collectors.toSet()));
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
            Path targetPath = cachedFilePath.resolve(workingDirectory.relativize(path));
            Files.createDirectories(targetPath.getParent());

            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }

      for (Map.Entry<String, String> entry : information.getServiceVersion().getAdditionalDownloads().entrySet()) {
        ConsoleProgressWrappers.wrapDownload(entry.getKey(), stream -> {
          try (OutputStream out = information.getTemplateStorage().newOutputStream(entry.getKey())) {
            FileUtils.copy(stream, out);
          }
        });
      }

      return true;
    } catch (Exception exception) {
      LOGGER.severe("Exception while installing application files", exception);
    } finally {
      FileUtils.delete(workingDirectory);
    }

    return false;
  }

  @UnmodifiableView
  public @NotNull Map<String, ServiceVersionType> getServiceVersionTypes() {
    return Collections.unmodifiableMap(this.serviceVersionTypes);
  }
}
