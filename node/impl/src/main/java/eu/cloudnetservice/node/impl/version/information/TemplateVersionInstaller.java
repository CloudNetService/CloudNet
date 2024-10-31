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

package eu.cloudnetservice.node.impl.version.information;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.node.impl.version.ServiceVersion;
import eu.cloudnetservice.node.impl.version.ServiceVersionType;
import eu.cloudnetservice.utils.base.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class TemplateVersionInstaller extends VersionInstaller {

  private final ServiceTemplate serviceTemplate;
  private final TemplateStorage templateStorage;

  public TemplateVersionInstaller(
    @NonNull ServiceVersion serviceVersion,
    @NonNull ServiceVersionType serviceVersionType,
    boolean cacheFiles,
    @Nullable String installerExecutable,
    @NonNull ServiceTemplate serviceTemplate,
    @NonNull TemplateStorage templateStorage
  ) {
    super(serviceVersion, serviceVersionType, cacheFiles, installerExecutable);

    this.serviceTemplate = serviceTemplate;
    this.templateStorage = templateStorage;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  public void deployFile(@NonNull InputStream source, @NonNull String target) throws IOException {
    try (var output = this.templateStorage.newOutputStream(this.serviceTemplate, target)) {
      if (output != null) {
        source.transferTo(output);
      }
    }
  }

  @Override
  public void removeServiceVersions(@NonNull Collection<ServiceVersionType> knownTypes) {
    for (var file : this.templateStorage.listFiles(this.serviceTemplate, "", false)) {
      if (file != null) {
        for (var environment : knownTypes) {
          if (StringUtil.toLower(file.name()).contains(environment.name()) && file.name().endsWith(".jar")) {
            this.templateStorage.deleteFile(this.serviceTemplate, file.path());
          }
        }
      }
    }
  }

  public static final class Builder extends VersionInstaller.Builder<TemplateVersionInstaller, Builder> {

    private ServiceTemplate serviceTemplate;
    private TemplateStorage templateStorage;

    public @NonNull Builder toTemplate(@NonNull ServiceTemplate template) {
      this.serviceTemplate = template;
      this.templateStorage = template.storage();

      return this;
    }

    public @NonNull Builder storage(@NonNull TemplateStorage storage) {
      this.templateStorage = storage;
      return this;
    }

    @Override
    protected @NonNull TemplateVersionInstaller doBuild() {
      Preconditions.checkNotNull(this.serviceTemplate, "no service template given");
      Preconditions.checkNotNull(this.templateStorage, "no template storage given");

      return new TemplateVersionInstaller(
        this.serviceVersion,
        this.serviceVersionType,
        this.cacheFiles,
        this.installerExecutable,
        this.serviceTemplate,
        this.templateStorage);
    }
  }
}
