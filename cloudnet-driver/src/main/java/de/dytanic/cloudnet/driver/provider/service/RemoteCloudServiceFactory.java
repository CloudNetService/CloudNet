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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteCloudServiceFactory extends DefaultCloudServiceFactory implements CloudServiceFactory,
  DriverAPIUser {

  private final Supplier<INetworkChannel> channelSupplier;

  public RemoteCloudServiceFactory(Supplier<INetworkChannel> channelSupplier) {
    this.channelSupplier = channelSupplier;
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
    return this.createCloudServiceAsync(serviceConfiguration).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
    Preconditions.checkNotNull(serviceConfiguration);

    return this.executeDriverAPIMethod(
      DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_CONFIGURATION,
      buffer -> buffer.writeObject(serviceConfiguration),
      packet -> packet.getBuffer().readOptionalObject(ServiceInfoSnapshot.class)
    );
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.channelSupplier.get();
  }
}
