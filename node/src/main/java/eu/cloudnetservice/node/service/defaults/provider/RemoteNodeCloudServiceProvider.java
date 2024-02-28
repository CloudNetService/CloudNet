/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.service.defaults.provider;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

public abstract class RemoteNodeCloudServiceProvider implements SpecificCloudServiceProvider {

  private volatile ServiceInfoSnapshot snapshot;

  public RemoteNodeCloudServiceProvider(@NonNull ServiceInfoSnapshot snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public @NonNull ServiceInfoSnapshot serviceInfo() {
    return this.snapshot;
  }

  @Override
  public @NonNull Task<ServiceInfoSnapshot> serviceInfoAsync() {
    return Task.completedTask(this.snapshot);
  }

  public void snapshot(@NonNull ServiceInfoSnapshot snapshot) {
    this.snapshot = snapshot;
  }
}
