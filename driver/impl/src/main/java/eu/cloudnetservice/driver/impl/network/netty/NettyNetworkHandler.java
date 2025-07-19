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

package eu.cloudnetservice.driver.impl.network.netty;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default netty inbound handler used to call downstream packet listeners when receiving a packet.
 *
 * @since 4.0
 */
public abstract class NettyNetworkHandler extends SimpleChannelInboundHandler<BasePacket> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyNetworkHandler.class);

  protected volatile NettyNetworkChannel channel;

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) throws Exception {
    this.channels().remove(this.channel);
    this.channel.handleClose();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelExceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException)) {
      LOGGER.error("Exception in network handler", cause);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelReadComplete(@NonNull ChannelHandlerContext ctx) {
    ctx.flush();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void messageReceived(@NonNull ChannelHandlerContext ctx, @NonNull BasePacket msg) {
    // post directly if the packet has a high priority
    if (msg.prioritized()) {
      this.handlePacket(msg);
    } else {
      this.packetDispatcher().execute(() -> this.handlePacket(msg));
    }
  }

  /**
   * Handles the incoming packet and posts it either to the associated waiting query handler or directly into the packet
   * registry, calling all associated handlers. This method applies exception handling which is not done by
   * {@link #doHandlePacket(BasePacket)}.
   *
   * @param packet the packet hto handle.
   * @throws NullPointerException if the given packet is null.
   */
  private void handlePacket(@NonNull BasePacket packet) {
    try {
      this.doHandlePacket(packet);
    } catch (Exception exception) {
      LOGGER.error("Exception whilst handling packet {}", packet, exception);
    }
  }

  /**
   * Handles the incoming packet and posts it either to the associated waiting query handler or directly into the packet
   * registry, calling all associated handlers.
   *
   * @param packet the packet to handle.
   * @throws NullPointerException if the given packet is null.
   * @throws Exception            if an exception occurs while handling the given packet.
   */
  protected void doHandlePacket(@NonNull BasePacket packet) throws Exception {
    var queryId = packet.uniqueId();
    if (queryId != null) {
      // the received packet is a query packet, either a response or a request. this only
      // handles if the received query message is a response. the packet content should
      // not be released here, as the content might be processed async by the handler
      var queryFuture = this.channel.queryPacketManager().waitingHandler(queryId);
      if (queryFuture != null) {
        var didComplete = queryFuture.complete(packet);
        if (!didComplete) {
          packet.content().release();
        }

        return;
      }
    }

    // post the packet to a packet handler and release the packet content after. a handler
    // must acquire the packet content if async processing is being done
    try {
      var packetHandlingAllowed = this.channel.handler().handlePacketReceive(this.channel, packet);
      if (packetHandlingAllowed) {
        this.channel.packetRegistry().handlePacket(this.channel, packet);
      }
    } finally {
      packet.content().release();
    }
  }

  /**
   * Get all channels which are connected to the underlying network component.
   *
   * @return all connected channels.
   */
  protected abstract @NonNull Collection<NetworkChannel> channels();

  /**
   * Get the packet dispatcher used to dispatch incoming packets. Each dispatcher is normally bound to the network
   * component which opened/received the connection and requested this handler.
   *
   * @return the dispatcher used to dispatch packets.
   */
  protected abstract @NonNull Executor packetDispatcher();
}
