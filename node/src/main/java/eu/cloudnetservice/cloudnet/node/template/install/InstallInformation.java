/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.template.install;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.SpecificTemplateStorage;
import java.util.Optional;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record InstallInformation(
  @NonNull ServiceVersion serviceVersion,
  @NonNull ServiceVersionType serviceVersionType,
  boolean cacheFiles,
  @Nullable String installerExecutable,
  @NonNull ServiceTemplate serviceTemplate,
  @NonNull SpecificTemplateStorage templateStorage
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public @NonNull Optional<String> installerExecCommand() {
    return Optional.ofNullable(this.installerExecutable);
  }

  public static final class Builder {

    private ServiceVersion serviceVersion;
    private ServiceVersionType serviceVersionType;

    private boolean cacheFiles;
    private String installerExecutable;
    private ServiceTemplate serviceTemplate;
    private SpecificTemplateStorage templateStorage;

    public @NonNull Builder serviceVersion(@NonNull ServiceVersion serviceVersion) {
      this.serviceVersion = serviceVersion;
      this.cacheFiles = serviceVersion.cacheFiles();
      return this;
    }

    public @NonNull Builder serviceVersionType(@NonNull ServiceVersionType serviceVersionType) {
      this.serviceVersionType = serviceVersionType;
      return this;
    }

    public @NonNull Builder cacheFiles(boolean cacheFiles) {
      this.cacheFiles = cacheFiles;
      return this;
    }

    public @NonNull Builder executable(@Nullable String installerExecutable) {
      this.installerExecutable = installerExecutable;
      return this;
    }

    public @NonNull Builder toTemplate(@NonNull ServiceTemplate template) {
      this.serviceTemplate = template;
      this.templateStorage = template.storage();

      return this;
    }

    public @NonNull Builder storage(@NonNull SpecificTemplateStorage storage) {
      this.templateStorage = storage;
      return this;
    }

    public @NonNull InstallInformation build() {
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
