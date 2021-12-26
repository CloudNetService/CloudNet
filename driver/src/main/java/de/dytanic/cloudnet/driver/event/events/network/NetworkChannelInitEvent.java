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

import de.dytanic.cloudnet.driver.event.Cancelable;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import lombok.NonNull;

public class NetworkChannelInitEvent extends NetworkEvent implements Cancelable {

  private final ChannelType channelType;
  private boolean cancelled;

  public NetworkChannelInitEvent(@NonNull NetworkChannel channel, @NonNull ChannelType channelType) {
    super(channel);
    this.channelType = channelType;
  }

  public @NonNull ChannelType channelType() {
    return this.channelType;
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
