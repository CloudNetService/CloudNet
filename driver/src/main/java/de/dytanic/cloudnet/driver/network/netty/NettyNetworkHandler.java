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

package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.BasePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public abstract class NettyNetworkHandler extends SimpleChannelInboundHandler<BasePacket> {

  private static final Logger LOGGER = LogManager.logger(NettyNetworkHandler.class);

  protected volatile NettyNetworkChannel channel;

  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      if (this.channel.handler() != null) {
        this.channel.handler().handleChannelClose(this.channel);
      }

      ctx.channel().close();
      this.channels().remove(this.channel);
    }
  }

  @Override
  public void exceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException)) {
      LOGGER.severe("Exception in network handler", cause);
    }
  }

  @Override
  public void channelReadComplete(@NonNull ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(@NonNull ChannelHandlerContext ctx, @NonNull BasePacket msg) {
    this.packetDispatcher().execute(() -> {
      try {
        var uuid = msg.uniqueId();
        if (uuid != null) {
          var task = this.channel.queryPacketManager().waitingHandler(uuid);
          if (task != null) {
            task.complete(msg);
            // don't post a query response packet to another handler at all
            return;
          }
        }

        if (this.channel.handler() == null || this.channel.handler().handlePacketReceive(this.channel, msg)) {
          this.channel.packetRegistry().handlePacket(this.channel, msg);
        }
      } catch (Exception exception) {
        LOGGER.severe("Exception whilst handling packet " + msg, exception);
      }
    });
  }

  protected abstract @NonNull Collection<NetworkChannel> channels();

  protected abstract @NonNull Executor packetDispatcher();
}
