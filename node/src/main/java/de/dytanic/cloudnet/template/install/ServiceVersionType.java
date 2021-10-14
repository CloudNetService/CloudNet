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

import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.template.install.execute.InstallStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ServiceVersionType {

  private String name;
  private Collection<ServiceVersion> versions;
  private ServiceEnvironment targetEnvironment;
  private List<InstallStep> installSteps = new ArrayList<>();

  public ServiceVersionType() {
  }

  public ServiceVersionType(
    @NotNull String name,
    @NotNull ServiceEnvironment targetEnvironment,
    @NotNull List<InstallStep> installSteps,
    @NotNull Collection<ServiceVersion> versions
  ) {
    this.name = name;
    this.targetEnvironment = targetEnvironment;
    this.installSteps = installSteps;
    this.versions = versions;
  }

  public Optional<ServiceVersion> getVersion(String name) {
    return this.versions.stream()
      .filter(serviceVersion -> serviceVersion.getName().equalsIgnoreCase(name))
      .findFirst();
  }

  public boolean canInstall(ServiceVersion serviceVersion) {
    return !this.installSteps.contains(InstallStep.BUILD) || serviceVersion.canRun();
  }

  public boolean canInstall(ServiceVersion serviceVersion, JavaVersion javaVersion) {
    return !this.installSteps.contains(InstallStep.BUILD) || serviceVersion.canRun(javaVersion);
  }

  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull ServiceEnvironment getTargetEnvironment() {
    return this.targetEnvironment;
  }

  public @NotNull List<InstallStep> getInstallSteps() {
    return this.installSteps;
  }

  public @NotNull Collection<ServiceVersion> getVersions() {
    return this.versions;
  }
}
