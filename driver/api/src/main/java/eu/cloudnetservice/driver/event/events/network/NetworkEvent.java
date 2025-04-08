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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.network.NetworkChannel;
import lombok.NonNull;

/**
 * Represents any event which is related to any network channel connected to the current network component.
 *
 * @since 4.0
 */
public abstract class NetworkEvent extends Event {

  private final NetworkChannel channel;

  /**
   * Creates a new instance of this network event.
   *
   * @param channel the channel which is associated with this event.
   * @throws NullPointerException if the given channel is null.
   */
  public NetworkEvent(@NonNull NetworkChannel channel) {
    this.channel = channel;
  }

  /**
   * Get the channel which is associated with this event.
   *
   * @return the channel.
   */
  public @NonNull NetworkChannel networkChannel() {
    return this.channel;
  }
}
