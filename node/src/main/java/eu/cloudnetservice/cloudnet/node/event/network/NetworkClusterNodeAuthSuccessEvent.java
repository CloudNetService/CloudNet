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

package eu.cloudnetservice.cloudnet.node.event.network;

import eu.cloudnetservice.cloudnet.driver.event.events.network.NetworkEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.node.cluster.ClusterNodeServer;
import lombok.NonNull;

public final class NetworkClusterNodeAuthSuccessEvent extends NetworkEvent {

  private final ClusterNodeServer node;

  public NetworkClusterNodeAuthSuccessEvent(@NonNull ClusterNodeServer node, @NonNull NetworkChannel channel) {
    super(channel);
    this.node = node;
  }

  public @NonNull ClusterNodeServer node() {
    return this.node;
  }
}
