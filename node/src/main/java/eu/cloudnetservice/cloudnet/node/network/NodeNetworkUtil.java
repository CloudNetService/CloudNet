/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.network;

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.events.network.ChannelType;
import eu.cloudnetservice.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.chunk.defaults.factory.EventChunkHandlerFactory;
import eu.cloudnetservice.cloudnet.driver.network.chunk.network.ChunkedPacketListener;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.cloudnet.driver.network.rpc.listener.RPCPacketListener;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.ClusterNodeServer;
import eu.cloudnetservice.cloudnet.node.network.listener.PacketServerChannelMessageListener;
import lombok.NonNull;

public final class NodeNetworkUtil {

  private static final Logger LOGGER = LogManager.logger(NodeNetworkUtil.class);

  private NodeNetworkUtil() {
    throw new UnsupportedOperationException();
  }

  static boolean shouldInitializeChannel(@NonNull NetworkChannel channel, @NonNull ChannelType type) {
    return !CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelInitEvent(channel, type)).cancelled();
  }

  public static void closeNodeServer(@NonNull ClusterNodeServer clusterNodeServer) {
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
