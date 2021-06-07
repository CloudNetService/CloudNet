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

package de.dytanic.cloudnet.driver.network.netty.http;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyHttpServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private final NettyHttpServer nettyHttpServer;

  private final HostAndPort connectedAddress;

  private NettyHttpChannel channel;

  public NettyHttpServerHandler(NettyHttpServer nettyHttpServer, HostAndPort connectedAddress) {
    this.nettyHttpServer = nettyHttpServer;
    this.connectedAddress = connectedAddress;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    this.channel = new NettyHttpChannel(ctx.channel(), this.connectedAddress,
      HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      ctx.channel().close();
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
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
    if (msg.decoderResult().isFailure()) {
      ctx.channel().close();
      return;
    }

    this.handleMessage(ctx.channel(), msg);
  }

  private void handleMessage(Channel channel, HttpRequest httpRequest) {
    URI uri = URI.create(httpRequest.uri());
    String fullPath = uri.getPath();

    if (fullPath.isEmpty()) {
      fullPath = "/";
    }
    if (fullPath.endsWith("/") && !fullPath.equals("/")) {
      fullPath = fullPath.substring(0, fullPath.length() - 1);
    }

    Map<String, String> pathParameters = new HashMap<>();

    List<NettyHttpServer.HttpHandlerEntry> entries = new ArrayList<>(this.nettyHttpServer.registeredHandlers);
    Collections.sort(entries);

    String[] pathEntries = fullPath.split("/");
    String[] handlerPathEntries;

    NettyHttpServerContext context = new NettyHttpServerContext(this.nettyHttpServer, this.channel, uri, pathParameters,
      httpRequest);

    for (NettyHttpServer.HttpHandlerEntry httpHandlerEntry : entries) {
      if (context.cancelNext) {
        break;
      }
      handlerPathEntries = httpHandlerEntry.path.split("/");

      if (this.handleMessage0(httpHandlerEntry, context, pathParameters, fullPath, pathEntries, handlerPathEntries)) {
        context.lastHandler = httpHandlerEntry.httpHandler;
      }
    }

    if (!context.cancelSendResponse) {
      if (context.httpServerResponse.statusCode() == 404
        && context.httpServerResponse.httpResponse.content().readableBytes() == 0) {
        context.httpServerResponse.httpResponse.content().writeBytes("Resource not found!".getBytes());
      }

      ChannelFuture channelFuture = channel.writeAndFlush(context.httpServerResponse.httpResponse)
        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

      if (context.closeAfter()) {
        channelFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private boolean handleMessage0(NettyHttpServer.HttpHandlerEntry httpHandlerEntry, NettyHttpServerContext context,
    Map<String, String> pathParameters, String fullPath, String[] pathEntries, String[] handlerPathEntries) {
    if (httpHandlerEntry.port != null && httpHandlerEntry.port != this.connectedAddress.getPort()) {
      return false;
    }

    if (!httpHandlerEntry.path.endsWith("*") && pathEntries.length != handlerPathEntries.length) {
      return false;
    }

    if (pathEntries.length < handlerPathEntries.length) {
      return false;
    }

    boolean wildCard = false;

    if (!(pathEntries.length == 1 && handlerPathEntries.length == 1)) {
      for (int index = 1; index < pathEntries.length; ++index) {
        if (wildCard) {
          continue;
        }

        if (index >= handlerPathEntries.length) {
          return false;
        }

        if (handlerPathEntries[index].equals("*") && handlerPathEntries.length - 1 == index) {
          wildCard = true;
          continue;
        }

        if (handlerPathEntries[index].startsWith("{") && handlerPathEntries[index].endsWith("}")
          && handlerPathEntries[index].length() > 2) {
          String replacedString = handlerPathEntries[index].replaceFirst("\\{", "");
          pathParameters.put(replacedString.substring(0, replacedString.length() - 1), pathEntries[index]);
          continue;
        }

        if (handlerPathEntries[index].equals("*")) {
          continue;
        }

        if (!handlerPathEntries[index].equals(pathEntries[index])) {
          return false;
        }
      }
    }

    try {
      httpHandlerEntry.httpHandler.handle(fullPath, context);
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
    return true;
  }
}
