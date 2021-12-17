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
  @NotNull ServiceInfoSnapshot serviceInfo();

  @NotNull
  String runtime();

  @NotNull Queue<ServiceRemoteInclusion> waitingIncludes();

  @NotNull Queue<ServiceTemplate> waitingTemplates();

  @NotNull Queue<ServiceDeployment> waitingDeployments();

  @NotNull
  ServiceLifeCycle lifeCycle();

  @NotNull
  ICloudServiceManager cloudServiceManager();

  @NotNull
  ServiceConfiguration serviceConfiguration();

  @NotNull
  ServiceId serviceId();

  @NotNull
  String connectionKey();

  @NotNull
  Path directory();

  @Nullable
  INetworkChannel networkChannel();

  @Internal
  void networkChannel(@Nullable INetworkChannel channel);

  @NotNull
  ServiceInfoSnapshot lastServiceInfoSnapshot();

  @NotNull
  IServiceConsoleLogCache serviceConsoleLogCache();

  void doDelete();

  boolean alive();

  void publishServiceInfoSnapshot();

  @Internal
  void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
