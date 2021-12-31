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

package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.network.NetworkChannel;
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
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

public interface CloudService extends SpecificCloudServiceProvider {

  @Override
  @NonNull ServiceInfoSnapshot serviceInfo();

  @NonNull
  String runtime();

  @NonNull Queue<ServiceRemoteInclusion> waitingIncludes();

  @NonNull Queue<ServiceTemplate> waitingTemplates();

  @NonNull Queue<ServiceDeployment> waitingDeployments();

  @NonNull
  ServiceLifeCycle lifeCycle();

  @NonNull
  CloudServiceManager cloudServiceManager();

  @NonNull
  ServiceConfiguration serviceConfiguration();

  @NonNull
  ServiceId serviceId();

  @NonNull
  String connectionKey();

  @NonNull
  Path directory();

  @Nullable
  NetworkChannel networkChannel();

  @Internal
  void networkChannel(@Nullable NetworkChannel channel);

  @NonNull
  ServiceInfoSnapshot lastServiceInfoSnapshot();

  @NonNull
  ServiceConsoleLogCache serviceConsoleLogCache();

  void doDelete();

  boolean alive();

  void publishServiceInfoSnapshot();

  @Internal
  void updateServiceInfoSnapshot(@NonNull ServiceInfoSnapshot serviceInfoSnapshot);
}
