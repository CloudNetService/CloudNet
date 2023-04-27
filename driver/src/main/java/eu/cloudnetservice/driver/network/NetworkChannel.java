/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.protocol.PacketSender;
import eu.cloudnetservice.driver.network.protocol.QueryPacketManager;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A network channel represents an open connection from/to server.
 *
 * @since 4.0
 */
public interface NetworkChannel extends PacketSender {

  /**
   * Get a unique numeric id of this channel. The ids are starting at 1.
   *
   * @return a unique numeric id of this channel.
   */
  long channelId();

  /**
   * Get the server host from/to the channel is connected.
   *
   * @return the server address of the channel.
   */
  @NonNull HostAndPort serverAddress();

  /**
   * Get the client host from/to the channel is connected.
   *
   * @return the client address of the channel.
   */
  HostAndPort clientAddress();

  /**
   * Get the handler of this channel listening to all kind of operations on this channel.
   *
   * @return the listener for channel operations.
   */
  @NonNull NetworkChannelHandler handler();

  /**
   * Get the packet listener registry responsible for this channel.
   *
   * @return the packet listener registry responsible for this channel.
   */
  @NonNull PacketListenerRegistry packetRegistry();

  /**
   * Get the query packet manager of this channel.
   *
   * @return the query packet manager of this channel.
   */
  @NonNull QueryPacketManager queryPacketManager();

  /**
   * Get if this client is opened by a client rather than a server.
   *
   * @return true if this channel was opened by a client, false otherwise.
   */
  boolean clientProvidedChannel();

  /**
   * Converts and sends the given packet as a query into this channel, blocking the current thread until a response is
   * available or a timeout of 30 seconds was reached.
   *
   * @param packet the packet to send as a query.
   * @return the response to the query or null if no response was received in time.
   * @throws NullPointerException if the given packet is null.
   */
  @Nullable Packet sendQuery(@NonNull Packet packet);

  /**
   * Converts and sends the given packet as a query into this channel, returning a future either completed with the
   * response to the query or an exception if no response to the packet was received in time.
   *
   * @param packet the packet to send as a query.
   * @return a future completed with the result of the query or an exception in case of a timeout.
   * @throws NullPointerException if the given packet is null.
   */
  @NonNull Task<Packet> sendQueryAsync(@NonNull Packet packet);

  /**
   * Get if the underlying channel is currently writeable and will perform writes to the channel immediately.
   *
   * @return true if the channel can instantly process i/o requests, false otherwise.
   */
  boolean writeable();

  /**
   * Get if the underlying channel is still active and therefore connected.
   *
   * @return true if the channel is connected and active, false otherwise.
   */
  boolean active();

  /**
   * Requests the close of the channel, flushing all outbound i/o requests before. After a channel was closed it cannot
   * be used again.
   */
  void close();
}
