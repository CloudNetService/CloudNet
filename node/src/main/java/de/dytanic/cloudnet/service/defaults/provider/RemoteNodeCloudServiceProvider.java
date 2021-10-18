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

import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.RemoteSpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.NotNull;

public class RemoteNodeCloudServiceProvider extends RemoteSpecificCloudServiceProvider {

  private volatile ServiceInfoSnapshot snapshot;

  public RemoteNodeCloudServiceProvider(
    @NotNull GeneralCloudServiceProvider provider,
    @NotNull RPCSender providerSender,
    @NotNull ServiceInfoSnapshot snapshot
  ) {
    super(provider, providerSender, snapshot.getServiceId().getUniqueId());
    this.snapshot = snapshot;
  }

  @Override
  public @NotNull ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.snapshot;
  }

  @Override
  public @NotNull ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync() {
    return CompletedTask.done(this.snapshot);
  }

  public void setSnapshot(@NotNull ServiceInfoSnapshot snapshot) {
    this.snapshot = snapshot;
  }
}
