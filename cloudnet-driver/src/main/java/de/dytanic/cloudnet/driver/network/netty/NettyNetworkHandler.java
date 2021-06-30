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

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class NettyNetworkHandler extends SimpleChannelInboundHandler<Packet> {

  protected NettyNetworkChannel channel;

  protected abstract Collection<INetworkChannel> getChannels();

  protected abstract Executor getPacketDispatcher();

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      if (this.channel.getHandler() != null) {
        this.channel.getHandler().handleChannelClose(this.channel);
      }

      ctx.channel().close();
      this.getChannels().remove(this.channel);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (!(cause instanceof IOException)) {
      cause.printStackTrace();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
    this.getPacketDispatcher().execute(() -> {
      try {
        if (this.channel.getHandler() == null || this.channel.getHandler().handlePacketReceive(this.channel, msg)) {
          this.channel.getPacketRegistry().handlePacket(this.channel, msg);
        }
      } catch (Exception exception) {
        CloudNetDriver.getInstance().getLogger().error("Exception whilst handling packet " + msg, exception);
      }
    });
  }
}
