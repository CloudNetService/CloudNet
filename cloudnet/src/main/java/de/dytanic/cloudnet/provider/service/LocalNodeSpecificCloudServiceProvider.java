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

package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalNodeSpecificCloudServiceProvider implements SpecificCloudServiceProvider {

  private final CloudNet cloudNet;
  private final ICloudService service;

  public LocalNodeSpecificCloudServiceProvider(CloudNet cloudNet, ICloudService service) {
    this.cloudNet = cloudNet;
    this.service = service;
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.service.getServiceInfoSnapshot();
  }

  @Override
  public boolean isValid() {
    return this.service.getLifeCycle() != ServiceLifeCycle.DELETED;
  }

  @Override
  public ServiceInfoSnapshot forceUpdateServiceInfo() {
    return this.forceUpdateServiceInfoAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync() {
    return this.cloudNet.scheduleTask(this::getServiceInfoSnapshot);
  }

  @Override
  public @NotNull ITask<Boolean> isValidAsync() {
    return CompletedTask.create(this.isValid());
  }

  @Override
  public @NotNull ITask<ServiceInfoSnapshot> forceUpdateServiceInfoAsync() {
    return this.service.forceUpdateServiceInfoSnapshotAsync();
  }

  @Override
  public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
    Preconditions.checkNotNull(serviceTemplate);

    this.service.offerTemplate(serviceTemplate);
  }

  @Override
  @NotNull
  public ITask<Void> addServiceTemplateAsync(@NotNull ServiceTemplate serviceTemplate) {
    return this.cloudNet.scheduleTask(() -> {
      this.addServiceTemplate(serviceTemplate);
      return null;
    });
  }

  @Override
  public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
    Preconditions.checkNotNull(serviceRemoteInclusion);

    this.service.offerInclusion(serviceRemoteInclusion);
  }

  @Override
  @NotNull
  public ITask<Void> addServiceRemoteInclusionAsync(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
    return this.cloudNet.scheduleTask(() -> {
      this.addServiceRemoteInclusion(serviceRemoteInclusion);
      return null;
    });
  }

  @Override
  public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
    Preconditions.checkNotNull(serviceDeployment);

    this.service.addDeployment(serviceDeployment);
  }

  @Override
  @NotNull
  public ITask<Void> addServiceDeploymentAsync(@NotNull ServiceDeployment serviceDeployment) {
    return this.cloudNet.scheduleTask(() -> {
      this.addServiceDeployment(serviceDeployment);
      return null;
    });
  }

  @Override
  public Queue<String> getCachedLogMessages() {
    return this.service.getServiceConsoleLogCache().getCachedLogMessages();
  }

  @Override
  @NotNull
  public ITask<Queue<String>> getCachedLogMessagesAsync() {
    return this.cloudNet.scheduleTask(this::getCachedLogMessages);
  }

  @Override
  public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
    Preconditions.checkNotNull(lifeCycle);

    switch (lifeCycle) {
      case RUNNING:
        try {
          this.service.start();
        } catch (Exception exception) {
          exception.printStackTrace();
        }
        break;
      case STOPPED:
        this.cloudNet.scheduleTask(() -> {
          this.service.stop();
          return null;
        });
        break;
      case DELETED:
        this.cloudNet.scheduleTask(() -> {
          this.service.delete();
          return null;
        });
        break;
      default:
        break;
    }
  }

  @Override
  @NotNull
  public ITask<Void> setCloudServiceLifeCycleAsync(@NotNull ServiceLifeCycle lifeCycle) {
    return this.cloudNet.scheduleTask(() -> {
      this.setCloudServiceLifeCycle(lifeCycle);
      return null;
    });
  }

  @Override
  public void restart() {
    try {
      this.service.restart();
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  @Override
  @NotNull
  public ITask<Void> restartAsync() {
    return this.cloudNet.scheduleTask(() -> {
      this.restart();
      return null;
    });
  }

  @Override
  public void kill() {
    try {
      this.service.kill();
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  @Override
  @NotNull
  public ITask<Void> killAsync() {
    return this.cloudNet.scheduleTask(() -> {
      this.kill();
      return null;
    });
  }

  @Override
  public void runCommand(@NotNull String command) {
    this.service.runCommand(command);
  }

  @Override
  @NotNull
  public ITask<Void> runCommandAsync(@NotNull String command) {
    return this.cloudNet.scheduleTask(() -> {
      this.runCommand(command);
      return null;
    });
  }

  @Override
  public void includeWaitingServiceTemplates() {
    this.service.includeTemplates();
  }

  @Override
  public void includeWaitingServiceInclusions() {
    this.service.includeInclusions();
  }

  @Override
  public void deployResources(boolean removeDeployments) {
    this.service.deployResources(removeDeployments);
  }

  @Override
  @NotNull
  public ITask<Void> includeWaitingServiceTemplatesAsync() {
    return this.cloudNet.scheduleTask(() -> {
      this.includeWaitingServiceTemplates();
      return null;
    });
  }

  @Override
  @NotNull
  public ITask<Void> includeWaitingServiceInclusionsAsync() {
    return this.cloudNet.scheduleTask(() -> {
      this.includeWaitingServiceInclusions();
      return null;
    });
  }

  @Override
  @NotNull
  public ITask<Void> deployResourcesAsync(boolean removeDeployments) {
    return this.cloudNet.scheduleTask(() -> {
      this.deployResources(removeDeployments);
      return null;
    });
  }
}
