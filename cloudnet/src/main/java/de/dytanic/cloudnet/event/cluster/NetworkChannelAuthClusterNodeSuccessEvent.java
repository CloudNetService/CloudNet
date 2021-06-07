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

package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

public final class NetworkChannelAuthClusterNodeSuccessEvent extends Event {

  private final IClusterNodeServer node;

  private final INetworkChannel channel;

  public NetworkChannelAuthClusterNodeSuccessEvent(IClusterNodeServer node, INetworkChannel channel) {
    this.node = node;
    this.channel = channel;
  }

  public IClusterNodeServer getNode() {
    return this.node;
  }

  public INetworkChannel getChannel() {
    return this.channel;
  }
}
