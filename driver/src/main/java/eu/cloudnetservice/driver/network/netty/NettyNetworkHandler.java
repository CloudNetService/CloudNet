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

package eu.cloudnetservice.driver.network.netty;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * The default netty inbound handler used to call downstream packet listeners when receiving a packet.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public abstract class NettyNetworkHandler extends SimpleChannelInboundHandler<BasePacket> {

  private static final Logger LOGGER = LogManager.logger(NettyNetworkHandler.class);

  protected final EventManager eventManager;
  protected volatile NettyNetworkChannel channel;

  /**
   * Constructs a new netty network handler instance.
   *
   * @param eventManager the event manager of the current component.
   * @throws NullPointerException if the given event manager is null.
   */
  protected NettyNetworkHandler(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      this.channel.handler().handleChannelClose(this.channel);

      ctx.channel().close();
      this.channels().remove(this.channel);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelExceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException)) {
      LOGGER.severe("Exception in network handler", cause);
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
      this.doHandlePacket(msg);
    } else {
      this.packetDispatcher().execute(() -> this.doHandlePacket(msg));
    }
  }

  /**
   * Handles the incoming packet and posts it either to the associated waiting query handler or directly into the packet
   * registry, calling all associated handlers.
   *
   * @param packet the packet to handle.
   * @throws NullPointerException if the given packet is null.
   */
  protected void doHandlePacket(@NonNull BasePacket packet) {
    try {
      var uuid = packet.uniqueId();
      if (uuid != null) {
        var task = this.channel.queryPacketManager().waitingHandler(uuid);
        if (task != null) {
          task.complete(packet);
          // don't post a query response packet to another handler at all
          return;
        }
      }

      // check if we're allowed to handle the packet
      if (this.channel.handler().handlePacketReceive(this.channel, packet)) {
        this.channel.packetRegistry().handlePacket(this.channel, packet);
      }
    } catch (Exception exception) {
      LOGGER.severe("Exception whilst handling packet %s", exception, packet);
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
