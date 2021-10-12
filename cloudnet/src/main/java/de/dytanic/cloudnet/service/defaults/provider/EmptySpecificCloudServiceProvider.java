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

package de.dytanic.cloudnet.service.defaults.provider;

import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EmptySpecificCloudServiceProvider implements SpecificCloudServiceProvider {

  public static final EmptySpecificCloudServiceProvider INSTANCE = new EmptySpecificCloudServiceProvider();

  private EmptySpecificCloudServiceProvider() {
  }

  @Override
  public @Nullable ServiceInfoSnapshot getServiceInfoSnapshot() {
    return null;
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return null;
  }

  @Override
  public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
  }

  @Override
  public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
  }

  @Override
  public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
  }

  @Override
  public Queue<String> getCachedLogMessages() {
    return new LinkedBlockingDeque<>();
  }

  @Override
  public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
  }

  @Override
  public void restart() {
  }

  @Override
  public void runCommand(@NotNull String command) {
  }

  @Override
  public void includeWaitingServiceTemplates() {
  }

  @Override
  public void includeWaitingServiceInclusions() {
  }

  @Override
  public void deployResources(boolean removeDeployments) {
  }
}
