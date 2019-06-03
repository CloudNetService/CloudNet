package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class NettyHttpServerHandler extends
    SimpleChannelInboundHandler<HttpRequest> {

  private final NettyHttpServer nettyHttpServer;

  private final HostAndPort connectedAddress;

  private NettyHttpChannel channel;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.channel = new NettyHttpChannel(ctx.channel(), connectedAddress,
        new HostAndPort(ctx.channel().remoteAddress()));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel()
        .isWritable()) {
      ctx.channel().close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    if (!(cause instanceof IOException)) {
      cause.printStackTrace();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg)
      throws Exception {
    if (msg.decoderResult() != DecoderResult.SUCCESS) {
      ctx.channel().close();
      return;
    }

    this.handleMessage(ctx.channel(), msg);
  }

  private void handleMessage(Channel channel, HttpRequest httpRequest)
      throws Exception {
    URI uri = URI.create(httpRequest.uri());
    String fullPath = uri.getPath();

    if (fullPath.isEmpty()) {
      fullPath = "/";
    }
    if (fullPath.endsWith("/") && !fullPath.equals("/")) {
      fullPath = fullPath.substring(0, fullPath.length() - 1);
    }

    Map<String, String> pathParameters = Maps.newHashMap();
    List<NettyHttpServer.HttpHandlerEntry> entries = Iterables
        .newArrayList(this.nettyHttpServer.registeredHandlers);
    String[] pathEntries = fullPath.split("/"), handlerPathEntries;
    Collections.sort(entries);

    NettyHttpServerContext context = new NettyHttpServerContext(
        this.nettyHttpServer, this.channel, uri, pathParameters, httpRequest);

    for (NettyHttpServer.HttpHandlerEntry httpHandlerEntry : entries) {
      if (context.cancelNext) {
        break;
      }
      handlerPathEntries = httpHandlerEntry.path.split("/");

      if (this
          .handleMessage0(httpHandlerEntry, context, pathParameters, fullPath,
              pathEntries, handlerPathEntries)) {
        context.lastHandler = httpHandlerEntry.httpHandler;
      }
    }

    if (!context.cancelSendResponse) {
      if (context.httpServerResponse.statusCode() == 404
          && context.httpServerResponse.httpResponse.content().readableBytes()
          == 0) {
        context.httpServerResponse.httpResponse.content()
            .writeBytes("Resource not found!".getBytes());
      }

      ChannelFuture channelFuture = channel
          .writeAndFlush(context.httpServerResponse.httpResponse)
          .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

      if (context.closeAfter()) {
        channelFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private boolean handleMessage0(
      NettyHttpServer.HttpHandlerEntry httpHandlerEntry,
      NettyHttpServerContext context,
      Map<String, String> pathParameters, String fullPath, String[] pathEntries,
      String[] handlerPathEntries) {
    if (httpHandlerEntry.port != null
        && httpHandlerEntry.port != connectedAddress.getPort()) {
      return false;
    }

    if (!httpHandlerEntry.path.endsWith("*")
        && pathEntries.length != handlerPathEntries.length) {
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

        if (handlerPathEntries[index].equals("*")
            && handlerPathEntries.length - 1 == index) {
          wildCard = true;
          continue;
        }

        if (handlerPathEntries[index].startsWith("{")
            && handlerPathEntries[index].endsWith("}")
            && handlerPathEntries[index].length() > 2) {
          String replacedString = handlerPathEntries[index]
              .replaceFirst("\\{", "");
          pathParameters
              .put(replacedString.substring(0, replacedString.length() - 1),
                  pathEntries[index]);
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
