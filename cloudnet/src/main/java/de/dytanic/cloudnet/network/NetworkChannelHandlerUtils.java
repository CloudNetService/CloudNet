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

package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

final class NetworkChannelHandlerUtils {

  private static final Logger LOGGER = LogManager.getLogger(NetworkChannelHandlerUtils.class);

  private NetworkChannelHandlerUtils() {
    throw new UnsupportedOperationException();
  }

  static boolean shouldInitializeChannel(INetworkChannel channel, ChannelType type) {
    return !CloudNetDriver.getInstance().getEventManager().callEvent(
      new NetworkChannelInitEvent(channel, type)).isCancelled();
  }

  public static void closeNodeServer(IClusterNodeServer clusterNodeServer) {
    try {
      clusterNodeServer.close();
    } catch (Exception exception) {
      LOGGER.severe("Exception while closing service", exception);
    }
  }
}
