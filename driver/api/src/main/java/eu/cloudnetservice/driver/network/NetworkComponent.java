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

package eu.cloudnetservice.driver.network;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.protocol.PacketSender;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a peer in any form within the network.
 *
 * @since 4.0
 */
public interface NetworkComponent extends PacketSender {

  /**
   * Get if this component has ssl enabled.
   *
   * @return true if this component has ssl enabled, false otherwise.
   */
  boolean sslEnabled();

  /**
   * Get an immutable collection of all channels which are associated with this component.
   *
   * @return an immutable collection of all channels which are associated with this component.
   */
  @NonNull
  @Unmodifiable
  Collection<NetworkChannel> channels();

  /**
   * Get the first channel which is known to this component or null if no channel is present.
   *
   * @return the first channel of component or null if no channel is present.
   */
  default @UnknownNullability NetworkChannel firstChannel() {
    return Iterables.getFirst(this.channels(), null);
  }

  /**
   * Get the dispatching executor for received packets in any channel which is bound to this network component.
   *
   * @return the dispatching executor for received packets.
   */
  @NonNull
  Executor packetDispatcher();

  /**
   * Get the packet listener registry which will be the root registry for all channels initialized by this component.
   *
   * @return the root packet registry for all associated channels.
   */
  @NonNull
  PacketListenerRegistry packetRegistry();

  /**
   * Closes all open connections associated with this network component.
   */
  void closeChannels();
}
