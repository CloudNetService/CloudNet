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

package de.dytanic.cloudnet.driver.network;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import java.util.Collection;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Includes the basic functions for the client and the server
 */
public interface INetworkComponent extends IPacketSender {

  /**
   * Get if this component has ssl enabled.
   *
   * @return if this component has ssl enabled.
   */
  boolean isSslEnabled();

  /**
   * Get an immutable collection of all channels which are associated with this component.
   *
   * @return an immutable collection of all channels which are associated with this component.
   */
  @NotNull
  @Unmodifiable Collection<INetworkChannel> getChannels();

  /**
   * Get the first channel which is known to this component or {@code null} if no channel is known.
   *
   * @return the first channel which is known to this component or {@code null} if no channel is known.
   */
  default @Nullable INetworkChannel getFirstChannel() {
    return Iterables.getFirst(this.getChannels(), null);
  }

  /**
   * Get the dispatching executor for received packets in this channel.
   *
   * @return the dispatching executor for received packets in this channel.
   */
  @NotNull Executor getPacketDispatcher();

  /**
   * Get the packet listener registry which will be the root registry for all channels initialized by this component.
   *
   * @return the root packet listener for all associated channels.
   */
  @NotNull IPacketListenerRegistry getPacketRegistry();

  /**
   * Closes all open connections associated with this network component.
   */
  void closeChannels();
}
