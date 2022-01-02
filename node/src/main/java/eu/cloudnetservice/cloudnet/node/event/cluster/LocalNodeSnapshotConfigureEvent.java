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

package eu.cloudnetservice.cloudnet.node.event.cluster;

import eu.cloudnetservice.cloudnet.driver.event.events.DriverEvent;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.NonNull;

public final class LocalNodeSnapshotConfigureEvent extends DriverEvent {

  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

  public LocalNodeSnapshotConfigureEvent(@NonNull NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
    this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
  }

  public @NonNull NetworkClusterNodeInfoSnapshot snapshot() {
    return this.networkClusterNodeInfoSnapshot;
  }
}
