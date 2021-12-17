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

import static com.google.gson.reflect.TypeToken.getParameterized;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.animation.progressbar.ConsoleProgressWrappers;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.template.install.execute.InstallStep;
import de.dytanic.cloudnet.template.listener.TemplatePrepareListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private static final Logger LOGGER = LogManager.logger(ServiceVersionProvider.class);

  private static final Path VERSION_CACHE_PATH = Path.of(
    System.getProperty("cloudnet.versioncache.path", "local/versioncache"));

  private static final int VERSIONS_FILE_VERSION = 3;

  private static final Type COL_SER_VERSION = getParameterized(Collection.class, ServiceVersionType.class).getType();
  private static final Type COL_ENV_TYPE = getParameterized(Collection.class, ServiceEnvironmentType.class).getType();

  private final Map<String, ServiceVersionType> serviceVersionTypes = new ConcurrentHashMap<>();
  private final Map<String, ServiceEnvironmentType> serviceEnvironmentTypes = new ConcurrentHashMap<>();

  public ServiceVersionProvider(@NotNull IEventManager eventManager) {
    eventManager.registerListener(new TemplatePrepareListener());
  }

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

    var document = JsonDocument.newDocument(inputStream);
    var fileVersion = document.getInt("fileVersion", -1);

    if (VERSIONS_FILE_VERSION == fileVersion && document.contains("versions")) {
      // load all service environments
      Collection<ServiceEnvironmentType> environments = document.get("environments", COL_ENV_TYPE);
      environments.forEach(this::registerServiceEnvironmentType);
      // load all service versions after the environment types as they need to be present
      Collection<ServiceVersionType> versions = document.get("versions", COL_SER_VERSION);
      versions.forEach(this::registerServiceVersionType);
      // successful load
      return true;
    }

    return false;
  }

  public void interruptInstallSteps() {
    for (var installStep : InstallStep.values()) {
      installStep.interrupt();
    }
  }

  public void registerServiceVersionType(@NotNull ServiceVersionType versionType) {
    // ensure that we know the target environment for the service version
    Verify.verify(
      this.getEnvironmentType(versionType.environmentType()).isPresent(),
      "Missing environment %s for service version %s",
      versionType.environmentType(),
      versionType.name());
    // register the service version
    this.serviceVersionTypes.put(versionType.name().toLowerCase(), versionType);
  }

  public @NotNull Optional<ServiceVersionType> getServiceVersionType(@NotNull String name) {
    return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
  }

  public void registerServiceEnvironmentType(@NotNull ServiceEnvironmentType environmentType) {
    this.serviceEnvironmentTypes.put(environmentType.name().toUpperCase(), environmentType);
  }

  public @NotNull Optional<ServiceEnvironmentType> getEnvironmentType(@NotNull String name) {
    return Optional.ofNullable(this.serviceEnvironmentTypes.get(name.toUpperCase()));
  }

  public boolean installServiceVersion(@NotNull InstallInformation information, boolean force) {
    var fullVersionIdentifier = String.format("%s-%s",
      information.serviceVersionType().name(),
      information.serviceVersion().name());

    if (!force
      && information.installerExecCommand().isEmpty()
      && !information.serviceVersionType().canInstall(information.serviceVersion())
    ) {
      throw new IllegalArgumentException(String.format(
        "Cannot run %s on %s",
        fullVersionIdentifier,
        JavaVersion.runtimeVersion().name()));
    }

    if (information.serviceVersion().isDeprecated()) {
      LOGGER.warning(I18n.trans("versions-installer-deprecated-version"));
    }

    try {
      // delete all old application files to prevent that they are used to start the service
      for (var file : information.templateStorage().listFiles("", false)) {
        if (file != null) {
          for (ServiceEnvironment environment : this.serviceVersionTypes.values()) {
            if (file.name().toLowerCase().contains(environment.name()) && file.name().endsWith(".jar")) {
              information.templateStorage().deleteFile(file.path());
            }
          }
        }
      }
    } catch (IOException exception) {
      LOGGER.severe("Exception while deleting old application files", exception);
    }

    var workingDirectory = FileUtils.createTempFile();
    var cachedFilePath = VERSION_CACHE_PATH.resolve(fullVersionIdentifier);

    try {
      if (information.cacheFiles() && Files.exists(cachedFilePath)) {
        InstallStep.DEPLOY.execute(information, cachedFilePath, Files.walk(cachedFilePath).collect(Collectors.toSet()));
      } else {
        Files.createDirectories(workingDirectory);

        List<InstallStep> installSteps = new ArrayList<>(information.serviceVersionType().getInstallSteps());
        installSteps.add(InstallStep.DEPLOY);

        Set<Path> lastStepResult = new HashSet<>();
        for (var installStep : installSteps) {
          lastStepResult = installStep.execute(information, workingDirectory, lastStepResult);
        }

        if (information.serviceVersion().isCacheFiles()) {
          for (var path : lastStepResult) {
            var targetPath = cachedFilePath.resolve(workingDirectory.relativize(path));
            Files.createDirectories(targetPath.getParent());

            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }

      for (var entry : information.serviceVersion().getAdditionalDownloads().entrySet()) {
        ConsoleProgressWrappers.wrapDownload(entry.getKey(), stream -> {
          try (var out = information.templateStorage().newOutputStream(entry.getKey())) {
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

  @UnmodifiableView
  public @NotNull Map<String, ServiceEnvironmentType> getKnownEnvironments() {
    return Collections.unmodifiableMap(this.serviceEnvironmentTypes);
  }
}
