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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InstallInformation {

  private final ServiceVersion serviceVersion;
  private final ServiceVersionType serviceVersionType;

  private final boolean cacheFiles;
  private final String installerExecutable;
  private final ServiceTemplate serviceTemplate;
  private final SpecificTemplateStorage templateStorage;

  protected InstallInformation(
    @NotNull ServiceVersion serviceVersion,
    @NotNull ServiceVersionType serviceVersionType,
    boolean cacheFiles,
    @Nullable String installerExecutable,
    @NotNull ServiceTemplate serviceTemplate,
    @NotNull SpecificTemplateStorage templateStorage
  ) {
    this.serviceVersion = serviceVersion;
    this.serviceVersionType = serviceVersionType;
    this.cacheFiles = cacheFiles;
    this.installerExecutable = installerExecutable;
    this.serviceTemplate = serviceTemplate;
    this.templateStorage = templateStorage;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public @NotNull ServiceVersionType getServiceVersionType() {
    return this.serviceVersionType;
  }

  public @NotNull ServiceVersion getServiceVersion() {
    return this.serviceVersion;
  }

  public @NotNull SpecificTemplateStorage getTemplateStorage() {
    return this.templateStorage;
  }

  public boolean isCacheFiles() {
    return this.cacheFiles;
  }

  public @NotNull ServiceTemplate getServiceTemplate() {
    return this.serviceTemplate;
  }

  public @NotNull Optional<String> getInstallerExecutable() {
    return Optional.ofNullable(this.installerExecutable);
  }

  public static final class Builder {

    private ServiceVersion serviceVersion;
    private ServiceVersionType serviceVersionType;

    private boolean cacheFiles;
    private String installerExecutable;
    private ServiceTemplate serviceTemplate;
    private SpecificTemplateStorage templateStorage;

    public @NotNull Builder serviceVersion(@NotNull ServiceVersion serviceVersion) {
      this.serviceVersion = serviceVersion;
      this.cacheFiles = serviceVersion.isCacheFiles();
      return this;
    }

    public @NotNull Builder serviceVersionType(@NotNull ServiceVersionType serviceVersionType) {
      this.serviceVersionType = serviceVersionType;
      return this;
    }

    public @NotNull Builder cacheFiles(boolean cacheFiles) {
      this.cacheFiles = cacheFiles;
      return this;
    }

    public @NotNull Builder executable(@Nullable String installerExecutable) {
      this.installerExecutable = installerExecutable;
      return this;
    }

    public @NotNull Builder toTemplate(@NotNull ServiceTemplate template) {
      this.serviceTemplate = template;
      this.templateStorage = template.storage();

      return this;
    }

    public @NotNull Builder storage(@NotNull SpecificTemplateStorage storage) {
      this.templateStorage = storage;
      return this;
    }

    public @NotNull InstallInformation build() {
      Verify.verifyNotNull(this.serviceVersion, "No service version specified");
      Verify.verifyNotNull(this.serviceVersionType, "No version type specified");
      Verify.verifyNotNull(this.serviceTemplate, "No target template specified");
      Verify.verifyNotNull(this.templateStorage, "No target template storage specified");

      return new InstallInformation(
        this.serviceVersion,
        this.serviceVersionType,
        this.cacheFiles,
        this.installerExecutable,
        this.serviceTemplate,
        this.templateStorage);
    }
  }
}
