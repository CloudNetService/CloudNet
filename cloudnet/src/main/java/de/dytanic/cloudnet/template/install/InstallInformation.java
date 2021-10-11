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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InstallInformation {

  private final ServiceVersion serviceVersion;
  private final ServiceVersionType serviceVersionType;

  private String installerExecutable;
  private ServiceTemplate serviceTemplate;
  private SpecificTemplateStorage templateStorage;

  protected InstallInformation(@NotNull ServiceVersionType type, @NotNull ServiceVersion serviceVersion) {
    this.serviceVersionType = type;
    this.serviceVersion = serviceVersion;
  }

  public static @NotNull InstallInformation.Builder builder(
    @NotNull ServiceVersionType type,
    @NotNull ServiceVersion version
  ) {
    return new Builder(new InstallInformation(type, version));
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

  public @NotNull ServiceTemplate getServiceTemplate() {
    return this.serviceTemplate;
  }

  public @NotNull Optional<String> getInstallerExecutable() {
    return Optional.ofNullable(this.installerExecutable);
  }

  public static final class Builder {

    private final InstallInformation result;

    private Builder(InstallInformation result) {
      this.result = result;
    }

    public @NotNull Builder executable(@Nullable String installerExecutable) {
      this.result.installerExecutable = installerExecutable;
      return this;
    }

    public @NotNull Builder toTemplate(@NotNull ServiceTemplate template) {
      this.result.serviceTemplate = template;
      this.result.templateStorage = template.storage();

      return this;
    }

    public @NotNull Builder toStorage(@NotNull SpecificTemplateStorage storage) {
      this.result.templateStorage = storage;
      return this;
    }

    public @NotNull InstallInformation build() {
      Preconditions.checkNotNull(this.result.serviceTemplate, "No target template specified");
      Preconditions.checkNotNull(this.result.templateStorage, "No target template storage specified");

      return this.result;
    }
  }
}
