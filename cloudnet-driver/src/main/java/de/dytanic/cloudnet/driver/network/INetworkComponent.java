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

import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Includes the basic functions for the client and the server
 */
interface INetworkComponent extends IPacketSender {

  /**
   * Returns true if the network component allows to create a ssl connection
   */
  boolean isSslEnabled();

  /**
   * Returns all running enabled connections from the network component
   */
  Collection<INetworkChannel> getChannels();

  default INetworkChannel getFirstChannel() {
    Collection<INetworkChannel> channels = this.getChannels();
    return channels.isEmpty() ? null : channels.iterator().next();
  }

  Executor getPacketDispatcher();

  /**
   * Close all open connections from this network component
   */
  void closeChannels();

  /**
   * Returns the parent packet registry from all channels, that are this network component provide
   */
  IPacketListenerRegistry getPacketRegistry();
}
