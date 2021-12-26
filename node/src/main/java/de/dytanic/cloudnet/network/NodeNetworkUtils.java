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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.ClusterNodeServer;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.chunk.defaults.factory.EventChunkHandlerFactory;
import de.dytanic.cloudnet.driver.network.chunk.network.ChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.protocol.PacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.listener.RPCPacketListener;
import de.dytanic.cloudnet.network.listener.PacketServerChannelMessageListener;
import lombok.NonNull;

public final class NodeNetworkUtils {

  private static final Logger LOGGER = LogManager.logger(NodeNetworkUtils.class);

  private NodeNetworkUtils() {
    throw new UnsupportedOperationException();
  }

  static boolean shouldInitializeChannel(NetworkChannel channel, ChannelType type) {
    return !CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelInitEvent(channel, type)).cancelled();
  }

  public static void closeNodeServer(ClusterNodeServer clusterNodeServer) {
    try {
      clusterNodeServer.close();
    } catch (Exception exception) {
      LOGGER.severe("Exception while closing service", exception);
    }
  }

  public static void addDefaultPacketListeners(@NonNull PacketListenerRegistry registry, @NonNull CloudNet node) {
    registry.addListener(
      NetworkConstants.CHANNEL_MESSAGING_CHANNEL,
      new PacketServerChannelMessageListener(node.messenger(), node.eventManager()));
    registry.addListener(
      NetworkConstants.INTERNAL_RPC_COM_CHANNEL,
      new RPCPacketListener(node.rpcHandlerRegistry()));
    registry.addListener(
      NetworkConstants.CHUNKED_PACKET_COM_CHANNEL,
      new ChunkedPacketListener(EventChunkHandlerFactory.withDefaultEventManager()));
  }
}
