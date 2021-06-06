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

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
@ApiStatus.ScheduledForRemoval
public class ClusterNodeCloudServiceFactory extends RemoteCloudServiceFactory {

  private final IClusterNodeServer server;

  public ClusterNodeCloudServiceFactory(Supplier<INetworkChannel> channelSupplier, IClusterNodeServer server) {
    super(channelSupplier);
    this.server = server;
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
    return super.createCloudService(this.prepareConfiguration(serviceConfiguration));
  }

  @Override
  public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
    return super.createCloudServiceAsync(this.prepareConfiguration(serviceConfiguration));
  }

  private ServiceConfiguration prepareConfiguration(ServiceConfiguration serviceConfiguration) {
    serviceConfiguration.getServiceId().setNodeUniqueId(this.server.getNodeInfo().getUniqueId());
    return serviceConfiguration;
  }

}
