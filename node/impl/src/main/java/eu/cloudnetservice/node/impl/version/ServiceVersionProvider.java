/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.version;

import static io.leangen.geantyref.TypeFactory.parameterizedClass;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.node.impl.console.animation.progressbar.ConsoleProgressWrappers;
import eu.cloudnetservice.node.impl.template.listener.TemplatePrepareListener;
import eu.cloudnetservice.node.impl.version.execute.InstallStep;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ServiceVersionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceVersionProvider.class);
  private static final Path VERSION_CACHE_PATH = Path.of(System.getProperty(
    "cloudnet.versioncache.path",
    "local/versioncache"));

  private static final int VERSIONS_FILE_VERSION = 3;

  private static final Type COL_SER_VERSION = parameterizedClass(Collection.class, ServiceVersionType.class);
  private static final Type COL_ENV_TYPE = parameterizedClass(Collection.class, ServiceEnvironmentType.class);

  private final Map<String, ServiceVersionType> serviceVersionTypes = new ConcurrentHashMap<>();
  private final Map<String, ServiceEnvironmentType> serviceEnvironmentTypes = new ConcurrentHashMap<>();

  private final I18n i18n;
  private final ConsoleProgressWrappers consoleProgressWrappers;

  @Inject
  public ServiceVersionProvider(
    @NonNull @Service I18n i18n,
    @NonNull EventManager eventManager,
    @NonNull ConsoleProgressWrappers consoleProgressWrappers
  ) {
    this.i18n = i18n;
    this.consoleProgressWrappers = consoleProgressWrappers;

    eventManager.registerListener(new TemplatePrepareListener());
  }

  public boolean loadServiceVersionTypes(@NonNull String url) throws IOException {
    this.serviceVersionTypes.clear();

    return Unirest
      .get(url)
      .asObject(rawResponse -> {
        // only accept a 200 response
        if (rawResponse.getStatus() == 200) {
          return this.loadVersionsFromInputStream(rawResponse.getContent());
        }
        return false;
      })
      .getBody();
  }

  public void loadDefaultVersionTypes() {
    this.serviceVersionTypes.clear();
    this.loadVersionsFromInputStream(this.getClass().getClassLoader().getResourceAsStream("files/versions.json"));
  }

  private boolean loadVersionsFromInputStream(@Nullable InputStream inputStream) {
    if (inputStream == null) {
      return false;
    }

    var document = DocumentFactory.json().parse(inputStream);
    var fileVersion = document.getInt("fileVersion", -1);

    if (VERSIONS_FILE_VERSION == fileVersion && document.contains("versions")) {
      // load all service environments
      Collection<ServiceEnvironmentType> environments = document.readObject("environments", COL_ENV_TYPE);
      environments.forEach(this::registerServiceEnvironmentType);
      // load all service versions after the environment types as they need to be present
      Collection<ServiceVersionType> versions = document.readObject("versions", COL_SER_VERSION);
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

  public void registerServiceVersionType(@NonNull ServiceVersionType versionType) {
    // ensure that we know the target environment for the service version
    Preconditions.checkNotNull(
      this.environmentType(versionType.environmentType()),
      "Missing environment %s for service version %s",
      versionType.environmentType(),
      versionType.name());
    // register the service version
    this.serviceVersionTypes.put(StringUtil.toLower(versionType.name()), versionType);
  }

  public @Nullable ServiceVersionType serviceVersionType(@NonNull String name) {
    return this.serviceVersionTypes.get(StringUtil.toLower(name));
  }

  public void registerServiceEnvironmentType(@NonNull ServiceEnvironmentType environmentType) {
    this.serviceEnvironmentTypes.put(StringUtil.toUpper(environmentType.name()), environmentType);
  }

  public @Nullable ServiceEnvironmentType environmentType(@NonNull String name) {
    return this.serviceEnvironmentTypes.get(StringUtil.toUpper(name));
  }

  public boolean installServiceVersion(@NonNull VersionInstaller installer, boolean force) {
    var fullVersionIdentifier = String.format("%s-%s",
      installer.serviceVersionType().name(),
      installer.serviceVersion().name());

    if (!force
      && installer.installerExecutable() == null
      && !installer.serviceVersionType().canInstall(installer.serviceVersion())
    ) {
      throw new IllegalArgumentException(String.format(
        "Cannot run %s on %s",
        fullVersionIdentifier,
        JavaVersion.runtimeVersion().name()));
    }

    if (installer.serviceVersion().deprecated()) {
      LOGGER.warn(this.i18n.translate("versions-installer-deprecated-version"));
    }

    try {
      // delete all old application files to prevent that they are used to start the service
      installer.removeServiceVersions(this.serviceVersionTypes.values());
    } catch (IOException exception) {
      LOGGER.error("Exception while deleting old application files", exception);
    }

    var workingDirectory = FileUtil.createTempFile();
    var cachedFilePath = VERSION_CACHE_PATH.resolve(fullVersionIdentifier);

    try {
      if (installer.cacheFiles() && Files.exists(cachedFilePath)) {
        InstallStep.DEPLOY.execute(installer, cachedFilePath, Files.walk(cachedFilePath).collect(Collectors.toSet()));
      } else {
        Files.createDirectories(workingDirectory);

        List<InstallStep> installSteps = new ArrayList<>(installer.serviceVersionType().installSteps());
        installSteps.add(InstallStep.DEPLOY);

        Set<Path> lastStepResult = new HashSet<>();
        for (var installStep : installSteps) {
          lastStepResult = installStep.execute(installer, workingDirectory, lastStepResult);
        }

        if (installer.serviceVersion().cacheFiles()) {
          for (var path : lastStepResult) {
            var targetPath = cachedFilePath.resolve(workingDirectory.relativize(path));
            Files.createDirectories(targetPath.getParent());

            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }

      for (var entry : installer.serviceVersion().additionalDownloads().entrySet()) {
        this.consoleProgressWrappers.wrapDownload(entry.getValue(), in -> installer.deployFile(in, entry.getKey()));
      }

      return true;
    } catch (Exception exception) {
      LOGGER.error("Exception while installing application files", exception);
    } finally {
      FileUtil.delete(workingDirectory);
    }

    return false;
  }

  @UnmodifiableView
  public @NonNull Map<String, ServiceVersionType> serviceVersionTypes() {
    return Collections.unmodifiableMap(this.serviceVersionTypes);
  }

  @UnmodifiableView
  public @NonNull Map<String, ServiceEnvironmentType> knownEnvironments() {
    return Collections.unmodifiableMap(this.serviceEnvironmentTypes);
  }
}
