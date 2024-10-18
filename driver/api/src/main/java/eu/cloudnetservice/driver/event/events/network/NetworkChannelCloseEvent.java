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

package eu.cloudnetservice.driver.event.events.network;

import eu.cloudnetservice.driver.network.NetworkChannel;
import lombok.NonNull;

/**
 * An event being fired when a channel connected to either a client or server gets closed.
 *
 * @since 4.0
 */
public final class NetworkChannelCloseEvent extends NetworkEvent {

  private final ChannelType channelType;

  /**
   * Creates a new instance of this network event.
   *
   * @param channel     the channel which is associated with this event.
   * @param channelType the type of channel being closed.
   * @throws NullPointerException if the given channel or type is null.
   */
  public NetworkChannelCloseEvent(@NonNull NetworkChannel channel, @NonNull ChannelType channelType) {
    super(channel);
    this.channelType = channelType;
  }

  /**
   * Get the type of channel which got closed.
   *
   * @return the type of channel which got closed.
   */
  public @NonNull ChannelType channelType() {
    return this.channelType;
  }
}
