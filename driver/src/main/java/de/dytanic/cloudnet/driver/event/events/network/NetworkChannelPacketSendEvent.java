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

package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.NonNull;

public class NetworkChannelPacketSendEvent extends NetworkEvent implements ICancelable {

  private final IPacket packet;
  private boolean cancelled;

  public NetworkChannelPacketSendEvent(@NonNull INetworkChannel channel, @NonNull IPacket packet) {
    super(channel);
    this.packet = packet;
  }

  public @NonNull IPacket packet() {
    return this.packet;
  }

  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  @Override
  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
