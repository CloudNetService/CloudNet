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

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.stream.ChunkedStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    } else if (fullPath.endsWith("/")) {
      fullPath = fullPath.substring(0, fullPath.length() - 1);
    }

    List<NettyHttpServer.HttpHandlerEntry> entries = new ArrayList<>(this.nettyHttpServer.registeredHandlers);
    entries.sort(Comparator.comparingInt(entry -> entry.priority));

    String[] pathEntries = fullPath.split("/");
    NettyHttpServerContext context = new NettyHttpServerContext(this.nettyHttpServer, this.channel,
      uri, new HashMap<>(), httpRequest);

    for (NettyHttpServer.HttpHandlerEntry httpHandlerEntry : entries) {
      String[] handlerPathEntries = httpHandlerEntry.path.split("/");
      context.setPathPrefix(httpHandlerEntry.path);

      if (this.handleMessage0(httpHandlerEntry, context, fullPath, pathEntries, handlerPathEntries)) {
        context.setLastHandler(httpHandlerEntry.httpHandler);
        if (context.cancelNext()) {
          break;
        }
      }
    }

    if (!context.cancelSendResponse) {
      NettyHttpServerResponse response = context.httpServerResponse;
      if (response.statusCode() == 404 && !response.hasBody()) {
        response.body("Resource not found!");
      }

      FullHttpResponse netty = response.httpResponse;
      if (!context.closeAfter()) {
        netty.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      }

      ChannelFuture channelFuture;
      if (response.bodyStream() != null) {
        HttpUtil.setTransferEncodingChunked(netty, true);

        channel.write(new DefaultHttpResponse(netty.protocolVersion(), netty.status(), netty.headers()),
          channel.voidPromise());
        channelFuture = channel.writeAndFlush(new HttpChunkedInput(new ChunkedStream(response.bodyStream())),
          channel.newProgressivePromise());
      } else {
        netty.headers().set(HttpHeaderNames.CONTENT_LENGTH, netty.content().readableBytes());
        channelFuture = channel.writeAndFlush(netty);
      }

      channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
      if (context.closeAfter()) {
        channelFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private boolean handleMessage0(NettyHttpServer.HttpHandlerEntry httpHandlerEntry, NettyHttpServerContext context,
    String fullPath, String[] pathEntries, String[] handlerPathEntries) {
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
    if (pathEntries.length != 1 || handlerPathEntries.length != 1) {
      for (int index = 1; index < pathEntries.length; ++index) {
        if (wildCard) {
          continue;
        }

        // check if a wildcard is provided
        String entry = handlerPathEntries[index];
        if (entry.equals("*")) {
          if (handlerPathEntries.length - 1 == index) {
            wildCard = true;
          }
          continue;
        }
        // check for a path parameter in form {name}
        if (entry.startsWith("{") && entry.endsWith("}") && entry.length() > 2) {
          String replacedString = entry.replaceFirst("\\{", "");
          context.request().pathParameters().put(
            replacedString.substring(0, replacedString.length() - 1), pathEntries[index]);
          continue;
        }
        // check if the uri does match at the position
        if (!entry.equals(pathEntries[index])) {
          return false;
        }
      }
    }

    try {
      httpHandlerEntry.httpHandler.handle(fullPath.toLowerCase(), context);
      return true;
    } catch (Exception exception) {
      CloudNetDriver.getInstance().getLogger().error(String.format("Exception posting http request to handler %s",
        httpHandlerEntry.httpHandler.getClass().getName()), exception);
      return false;
    }
  }
}
