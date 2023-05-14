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

package eu.cloudnetservice.driver.network.netty.http;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpHandleException;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFutureListeners;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import io.netty5.handler.codec.http.DefaultHttpResponse;
import io.netty5.handler.codec.http.EmptyLastHttpContent;
import io.netty5.handler.codec.http.HttpChunkedInput;
import io.netty5.handler.codec.http.HttpHeaderNames;
import io.netty5.handler.codec.http.HttpHeaderValues;
import io.netty5.handler.codec.http.HttpRequest;
import io.netty5.handler.codec.http.HttpResponseStatus;
import io.netty5.handler.codec.http.HttpUtil;
import io.netty5.handler.stream.ChunkedStream;
import io.netty5.handler.timeout.ReadTimeoutException;
import io.netty5.util.concurrent.Future;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * The http server handler implementation responsible to handling http requests sent to the server and responding to
 * them.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class NettyHttpServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServerHandler.class);

  private final NettyHttpServer nettyHttpServer;
  private final HostAndPort connectedAddress;

  private NettyHttpChannel channel;

  /**
   * Constructs a new http server handler instance.
   *
   * @param nettyHttpServer  the http server associated with this handler.
   * @param connectedAddress the listener host and port associated with this handler.
   * @throws NullPointerException if the given server or host and port are null.
   */
  public NettyHttpServerHandler(@NonNull NettyHttpServer nettyHttpServer, @NonNull HostAndPort connectedAddress) {
    this.nettyHttpServer = nettyHttpServer;
    this.connectedAddress = connectedAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelActive(@NonNull ChannelHandlerContext ctx) {
    this.channel = new NettyHttpChannel(ctx.channel(), this.connectedAddress,
      HostAndPort.fromSocketAddress(ctx.channel().remoteAddress()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      ctx.channel().close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelExceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException) && !(cause instanceof ReadTimeoutException)) {
      LOGGER.severe("Exception caught during processing of http request", cause);
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
  protected void messageReceived(@NonNull ChannelHandlerContext ctx, @NonNull HttpRequest msg) {
    // validate that the request was actually decoded before processing
    if (msg.decoderResult().isFailure()) {
      ctx.channel().close();
      return;
    }

    this.handleMessage(ctx.channel(), msg);
  }

  /**
   * Handles an incoming http request, posting it to the correct handler while parsing everything from it beforehand.
   *
   * @param channel     the channel from which the request came.
   * @param httpRequest the decoded request to handle.
   * @throws NullPointerException if the given channel or request is null.
   */
  private void handleMessage(@NonNull Channel channel, @NonNull HttpRequest httpRequest) {
    // if an opaque uri is sent to the server we reject the request immediately as it does
    // not contain the required information to properly process the request (especially due
    // to the lack of path information which is the base of our internal handling)
    var uri = URI.create(httpRequest.uri());
    if (uri.isOpaque()) {
      channel
        .writeAndFlush(new DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.BAD_REQUEST))
        .addListener(channel, ChannelFutureListeners.CLOSE);
      return;
    }

    // make the path understandable for the handlers
    var fullPath = uri.getPath();
    if (!fullPath.equals("/") && fullPath.endsWith("/")) {
      fullPath = fullPath.substring(0, fullPath.length() - 1);
    }

    // get all handlers which should be tested if they can accept the http request
    var entries = new ArrayList<>(this.nettyHttpServer.registeredHandlers);
    entries.sort(Comparator.comparingInt(NettyHttpServer.HttpHandlerEntry::priority).reversed());

    // build the context around the http request
    var pathEntries = fullPath.split("/");
    var context = new NettyHttpServerContext(this.nettyHttpServer, this.channel, uri, new HashMap<>(), httpRequest);

    // loop over each handler, posting the message to the handlers which are matching the request uri
    for (var httpHandlerEntry : entries) {
      // prepare the context to post to the handler
      context.invocationHints.clear();
      context.pathPrefix(httpHandlerEntry.path());

      // check and post to the handler if matching
      var handlerPathEntries = httpHandlerEntry.path().split("/");
      if (this.handleMessage0(httpHandlerEntry, context, fullPath, pathEntries, handlerPathEntries)) {
        // update the last handler in the pipeline which handled the request
        context.pushChain(httpHandlerEntry.httpHandler());
        // stop processing the request if a handler requested that
        if (context.cancelNext) {
          break;
        }
      }
    }

    // check if the response set in the context should actually be transferred to the client
    if (!context.cancelSendResponse) {
      var response = context.httpServerResponse;
      // append a body message when no http body was set
      if (response.status() == HttpResponseCode.NOT_FOUND && !response.hasBody()) {
        response.body("Resource not found!");
      }

      // append the keep-alive header if requested
      var netty = response.httpResponse;
      if (!context.closeAfter) {
        netty.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      }

      // transfer the data chunked to the client if a response stream was set, indicating a huge data chunk
      Future<Void> future;
      if (response.bodyStream() != null) {
        // set the chunk transfer header
        HttpUtil.setTransferEncodingChunked(netty, true);

        // write the initial response to the client, use a void future as no monitoring is required
        channel.write(new DefaultHttpResponse(netty.protocolVersion(), netty.status(), netty.headers()));
        // write the actual content of the transfer into the channel using a progressive future
        future = channel.writeAndFlush(new HttpChunkedInput(
          new ChunkedStream(response.bodyStream()),
          new EmptyLastHttpContent(channel.bufferAllocator())));
      } else {
        // do not mark the request data as chunked
        HttpUtil.setTransferEncodingChunked(netty, false);
        // Set the content length of the response and transfer the data to the client
        HttpUtil.setContentLength(netty, netty.payload().readableBytes());
        future = channel.writeAndFlush(netty);
      }

      // add the listener that fires the exception if an error occurs during writing of the response
      future.addListener(channel, ChannelFutureListeners.FIRE_EXCEPTION_ON_FAILURE);
      // if a close is requested, close the channel after the write process finished (either by succeeding or failing)
      if (context.closeAfter) {
        future.addListener(channel, ChannelFutureListeners.CLOSE);
      }
    }
  }

  /**
   * Handles the incoming request and posts it the given handler if the data matches the settings of the handler.
   *
   * @param httpHandlerEntry   the handler to post to if everything matches.
   * @param context            the context of the request.
   * @param fullPath           the full requested path.
   * @param pathEntries        the entries one by one of the path uri.
   * @param handlerPathEntries the entries of the http handler.
   * @return true if the message was posted to the handler, false otherwise.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private boolean handleMessage0(
    @NonNull NettyHttpServer.HttpHandlerEntry httpHandlerEntry,
    @NonNull HttpContext context,
    @NonNull String fullPath,
    @NonNull String[] pathEntries,
    @NonNull String[] handlerPathEntries
  ) {
    var path = StringUtil.toLower(fullPath);

    // validate that the port (if given) of the server and entry are matching
    if (httpHandlerEntry.port() != null && httpHandlerEntry.port() != this.connectedAddress.port()) {
      return false;
    }

    // check if the path either ends with a wildcard or if the path entry length are matching
    if (!httpHandlerEntry.path().endsWith("*") && pathEntries.length != handlerPathEntries.length) {
      return false;
    }

    // wildcard path check, if the path handler expects more entries than the original requested path
    if (pathEntries.length < handlerPathEntries.length) {
      return false;
    }

    // validate that all path entries are matching
    if (pathEntries.length != 1 || handlerPathEntries.length != 1) {
      for (var index = 1; index < pathEntries.length; ++index) {
        // check if the current index of the handler entries is a wildcard char and continue without further checking
        var entry = handlerPathEntries[index];
        if (entry.equals("*")) {
          // special case: if the entry is the last entry of the path entries we can stop the check as it should
          // match all upcoming uri path parts
          if (handlerPathEntries.length - 1 == index) {
            break;
          }
          continue;
        }

        // check for a path parameter in the form {name}
        if (entry.startsWith("{") && entry.endsWith("}") && entry.length() > 2) {
          // remove the initial { and ending } and register the name with the provided value in the path
          // parameters in the context and continue.
          context.request().pathParameters().put(entry.substring(1, entry.length() - 1), pathEntries[index]);
          continue;
        }

        // check if the uri does match at the position
        if (!entry.equals(pathEntries[index])) {
          return false;
        }
      }
    }

    // post the context to the invocation handlers (if any registered)
    var preprocessors = httpHandlerEntry.httpHandler().preprocessors();
    if (!preprocessors.isEmpty()) {
      for (var preprocessor : preprocessors) {
        // process & check if the handler requested to cancel the request
        context = preprocessor.preProcessContext(path, context);
        if (context == null) {
          return false;
        }
      }
    }

    try {
      // post the request to the handler as it does match
      httpHandlerEntry.httpHandler().handle(path, context);
      return true;
    } catch (Throwable throwable) {
      // catch a http handle exception - that one is expected to be thrown
      if (throwable instanceof HttpHandleException handleException) {
        context.response()
          .status(handleException.responseCode())
          .body(Objects.requireNonNullElse(handleException.responseBody(), new byte[0]));
        return false;
      }

      // assume that the request was bad so that the handler was unable to handle it
      LOGGER.finer(
        "Exception posting http request to handler %s",
        throwable,
        httpHandlerEntry.httpHandler().getClass().getName());
      context.response().status(HttpResponseCode.BAD_REQUEST);
      // continue with the next handler in the chain
      return false;
    }
  }
}
