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

import eu.cloudnetservice.driver.event.Cancelable;
import eu.cloudnetservice.driver.network.NetworkChannel;
import lombok.NonNull;

/**
 * An event being fired when a network channel get initialized either on the client or server side.
 *
 * @since 4.0
 */
public final class NetworkChannelInitEvent extends NetworkEvent implements Cancelable {

  private final ChannelType channelType;
  private boolean cancelled;

  /**
   * Creates a new instance of this network event.
   *
   * @param channel     the channel which is associated with this event.
   * @param channelType the type of channel being initialized.
   * @throws NullPointerException if the given channel or type is null.
   */
  public NetworkChannelInitEvent(@NonNull NetworkChannel channel, @NonNull ChannelType channelType) {
    super(channel);
    this.channelType = channelType;
  }

  /**
   * Get the type of channel getting initialized.
   *
   * @return the type of channel getting initialized.
   */
  public @NonNull ChannelType channelType() {
    return this.channelType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
