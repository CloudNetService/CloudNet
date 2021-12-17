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

package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides methods to create and prepare services in the network.
 */
@RPCValidation
public interface CloudServiceFactory {

  /**
   * Creates and prepares a new cloud service
   *
   * @param serviceConfiguration the configuration for the new service
   * @return the info of the created service or null if the service couldn't be created
   */
  @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration);

  /**
   * Creates and prepares a new cloud service
   *
   * @param configuration the configuration for the new service
   * @return the info of the created service or null if the service couldn't be created
   */
  default @NonNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(@NonNull ServiceConfiguration configuration) {
    return CompletableTask.supply(() -> this.createCloudService(configuration));
  }
}
