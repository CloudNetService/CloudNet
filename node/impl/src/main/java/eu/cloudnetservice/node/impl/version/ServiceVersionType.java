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

import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.service.ServiceEnvironment;
import eu.cloudnetservice.node.impl.version.execute.InstallStep;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class ServiceVersionType extends ServiceEnvironment {

  private final List<InstallStep> installSteps;
  private final Collection<ServiceVersion> versions;

  public ServiceVersionType(
    @NonNull String name,
    @NonNull String environmentType,
    @NonNull List<InstallStep> installSteps,
    @NonNull Collection<ServiceVersion> versions
  ) {
    super(name, environmentType);

    this.installSteps = installSteps;
    this.versions = versions;
  }

  public @Nullable ServiceVersion version(@NonNull String name) {
    return this.versions.stream()
      .filter(serviceVersion -> serviceVersion.name().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  public boolean canInstall(@NonNull ServiceVersion serviceVersion) {
    return !this.installSteps.contains(InstallStep.BUILD) || serviceVersion.canRun();
  }

  public boolean canInstall(@NonNull ServiceVersion serviceVersion, @NonNull JavaVersion javaVersion) {
    return !this.installSteps.contains(InstallStep.BUILD) || serviceVersion.canRun(javaVersion);
  }

  public @NonNull List<InstallStep> installSteps() {
    return this.installSteps;
  }

  public @NonNull Collection<ServiceVersion> versions() {
    return this.versions;
  }
}
