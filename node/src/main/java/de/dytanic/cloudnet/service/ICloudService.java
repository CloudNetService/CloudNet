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

package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.nio.file.Path;
import java.util.Queue;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICloudService extends SpecificCloudServiceProvider {

  @Override
  @NotNull ServiceInfoSnapshot getServiceInfoSnapshot();

  @NotNull
  String getRuntime();

  @NotNull Queue<ServiceRemoteInclusion> getWaitingIncludes();

  @NotNull Queue<ServiceTemplate> getWaitingTemplates();

  @NotNull Queue<ServiceDeployment> getWaitingDeployments();

  @NotNull
  ServiceLifeCycle getLifeCycle();

  @NotNull
  ICloudServiceManager getCloudServiceManager();

  @NotNull
  ServiceConfiguration getServiceConfiguration();

  @NotNull
  ServiceId getServiceId();

  @NotNull
  String getConnectionKey();

  @NotNull
  Path getDirectory();

  @Nullable
  INetworkChannel getNetworkChannel();

  @Internal
  void setNetworkChannel(@Nullable INetworkChannel channel);

  @NotNull
  ServiceInfoSnapshot getLastServiceInfoSnapshot();

  @NotNull
  IServiceConsoleLogCache getServiceConsoleLogCache();

  void doDelete();

  boolean isAlive();

  void publishServiceInfoSnapshot();

  @Internal
  void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
