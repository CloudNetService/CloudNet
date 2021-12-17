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

import de.dytanic.cloudnet.driver.network.http.HttpVersion;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
final class NettyHttpServerRequest extends NettyHttpMessage implements IHttpRequest {

  private final NettyHttpServerContext context;

  private final URI uri;
  private final HttpRequest httpRequest;

  private final Map<String, String> pathParameters;
  private final Map<String, List<String>> queryParameters;

  private byte[] body;

  public NettyHttpServerRequest(
    @NotNull NettyHttpServerContext context,
    @NotNull HttpRequest httpRequest,
    @NotNull Map<String, String> pathParameters,
    @NotNull URI uri
  ) {
    this.context = context;
    this.httpRequest = httpRequest;
    this.uri = uri;
    this.pathParameters = pathParameters;
    this.queryParameters = new QueryStringDecoder(httpRequest.uri()).parameters();
  }

  @Override
  public @NotNull Map<String, String> pathParameters() {
    return this.pathParameters;
  }

  @Override
  public @NotNull String path() {
    return this.uri.getPath();
  }

  @Override
  public @NotNull String uri() {
    return this.httpRequest.uri();
  }

  @Override
  public @NotNull String method() {
    return this.httpRequest.method().name();
  }

  @Override
  public @NotNull Map<String, List<String>> queryParameters() {
    return this.queryParameters;
  }

  @Override
  public @NotNull IHttpContext context() {
    return this.context;
  }

  @Override
  public String header(@NotNull String name) {
    return this.httpRequest.headers().getAsString(name);
  }

  @Override
  public int headerAsInt(@NotNull String name) {
    return this.httpRequest.headers().getInt(name);
  }

  @Override
  public boolean headerAsBoolean(@NotNull String name) {
    return Boolean.parseBoolean(this.httpRequest.headers().get(name));
  }

  @Override
  public @NotNull IHttpRequest header(@NotNull String name, @NotNull String value) {
    this.httpRequest.headers().set(name, value);
    return this;
  }

  @Override
  public @NotNull IHttpRequest removeHeader(@NotNull String name) {
    this.httpRequest.headers().remove(name);
    return this;
  }

  @Override
  public @NotNull IHttpRequest clearHeaders() {
    this.httpRequest.headers().clear();
    return this;
  }

  @Override
  public boolean hasHeader(@NotNull String name) {
    return this.httpRequest.headers().contains(name);
  }

  @Override
  public @NotNull Map<String, String> headers() {
    Map<String, String> maps = new HashMap<>(this.httpRequest.headers().size());

    for (var key : this.httpRequest.headers().names()) {
      maps.put(key, this.httpRequest.headers().get(key));
    }

    return maps;
  }

  @Override
  public @NotNull HttpVersion version() {
    return super.versionFromNetty(this.httpRequest.protocolVersion());
  }

  @Override
  public @NotNull IHttpRequest version(@NotNull HttpVersion version) {
    this.httpRequest.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  @Override
  public byte[] body() {
    if (this.httpRequest instanceof FullHttpRequest) {
      if (this.body == null) {
        var httpRequest = (FullHttpRequest) this.httpRequest;
        var length = httpRequest.content().readableBytes();

        if (httpRequest.content().hasArray()) {
          this.body = httpRequest.content().array();
        } else {
          this.body = new byte[length];
          httpRequest.content().getBytes(httpRequest.content().readerIndex(), this.body);
        }
      }

      return this.body;
    }

    return new byte[0];
  }

  @Override
  public @NotNull String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  @Override
  public @NotNull IHttpRequest body(byte[] byteArray) {
    throw new UnsupportedOperationException("Unable to set body in request");
  }

  @Override
  public @NotNull IHttpRequest body(@NotNull String text) {
    return this.body(text.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public @Nullable InputStream bodyStream() {
    if (this.httpRequest instanceof FullHttpRequest) {
      return new ByteBufInputStream(((FullHttpRequest) this.httpRequest).content());
    } else {
      return null;
    }
  }

  @Override
  public @NotNull IHttpRequest body(@Nullable InputStream body) {
    throw new UnsupportedOperationException("Unable to set body in request");
  }

  @Override
  public boolean hasBody() {
    return this.httpRequest instanceof FullHttpRequest
      && ((FullHttpRequest) this.httpRequest).content().readableBytes() > 0;
  }
}
