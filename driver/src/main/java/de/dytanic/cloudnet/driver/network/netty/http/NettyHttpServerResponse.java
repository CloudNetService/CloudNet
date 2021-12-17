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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.http.HttpVersion;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

final class NettyHttpServerResponse extends NettyHttpMessage implements IHttpResponse {

  final DefaultFullHttpResponse httpResponse;
  private final NettyHttpServerContext context;

  private InputStream responseInputStream;

  public NettyHttpServerResponse(NettyHttpServerContext context, HttpRequest httpRequest) {
    this.context = context;
    this.httpResponse = new DefaultFullHttpResponse(
      httpRequest.protocolVersion(),
      HttpResponseStatus.NOT_FOUND,
      Unpooled.buffer()
    );
  }

  @Override
  public int statusCode() {
    return this.httpResponse.status().code();
  }

  @Override
  public @NotNull IHttpResponse statusCode(int code) {
    this.httpResponse.setStatus(HttpResponseStatus.valueOf(code));
    return this;
  }

  @Override
  public @NotNull IHttpContext context() {
    return this.context;
  }

  @Override
  public String header(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.httpResponse.headers().getAsString(name);
  }

  @Override
  public int headerAsInt(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.httpResponse.headers().getInt(name);
  }

  @Override
  public boolean headerAsBoolean(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return Boolean.parseBoolean(this.httpResponse.headers().get(name));
  }

  @Override
  public @NotNull IHttpResponse header(@NotNull String name, @NotNull String value) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(value);

    this.httpResponse.headers().set(name, value);
    return this;
  }

  @Override
  public @NotNull IHttpResponse removeHeader(@NotNull String name) {
    Preconditions.checkNotNull(name);
    this.httpResponse.headers().remove(name);
    return this;
  }

  @Override
  public @NotNull IHttpResponse clearHeaders() {
    this.httpResponse.headers().clear();
    return this;
  }

  @Override
  public boolean hasHeader(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.httpResponse.headers().contains(name);
  }

  @Override
  public @NotNull Map<String, String> headers() {
    Map<String, String> maps = new HashMap<>(this.httpResponse.headers().size());

    for (var key : this.httpResponse.headers().names()) {
      maps.put(key, this.httpResponse.headers().get(key));
    }

    return maps;
  }

  @Override
  public @NotNull HttpVersion version() {
    return super.versionFromNetty(this.httpResponse.protocolVersion());
  }

  @Override
  public @NotNull IHttpResponse version(@NotNull HttpVersion version) {
    Preconditions.checkNotNull(version);

    this.httpResponse.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  @Override
  public byte[] body() {
    return this.httpResponse.content().array();
  }

  @Override
  public @NotNull String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  @Override
  public @NotNull IHttpResponse body(byte[] byteArray) {
    Preconditions.checkNotNull(byteArray);

    this.httpResponse.content().clear();
    this.httpResponse.content().writeBytes(byteArray);
    return this;
  }

  @Override
  public @NotNull IHttpResponse body(@NotNull String text) {
    Preconditions.checkNotNull(text);

    this.httpResponse.content().clear();
    this.httpResponse.content().writeBytes(text.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  @Override
  public InputStream bodyStream() {
    return this.responseInputStream;
  }

  @Override
  public @NotNull IHttpResponse body(InputStream body) {
    if (this.responseInputStream != null) {
      try {
        this.responseInputStream.close();
      } catch (IOException ignored) {
      }
    }

    this.responseInputStream = body;
    return this;
  }

  @Override
  public boolean hasBody() {
    return this.httpResponse.content().readableBytes() > 0 || this.responseInputStream != null;
  }
}
