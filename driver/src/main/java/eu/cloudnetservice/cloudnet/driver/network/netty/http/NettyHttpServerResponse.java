/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.netty.http;

import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponse;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpVersion;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class NettyHttpServerResponse extends NettyHttpMessage implements HttpResponse {

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
  public @NonNull HttpResponseCode status() {
    return HttpResponseCode.fromNumeric(this.httpResponse.status().code());
  }

  @Override
  public @NonNull HttpResponse status(@NonNull HttpResponseCode code) {
    this.httpResponse.setStatus(HttpResponseStatus.valueOf(code.code()));
    return this;
  }

  @Override
  public @NonNull HttpContext context() {
    return this.context;
  }

  @Override
  public @Nullable String header(@NonNull String name) {
    return this.httpResponse.headers().getAsString(name);
  }

  @Override
  public @Nullable Integer headerAsInt(@NonNull String name) {
    return this.httpResponse.headers().getInt(name);
  }

  @Override
  public boolean headerAsBoolean(@NonNull String name) {
    return Boolean.parseBoolean(this.httpResponse.headers().get(name));
  }

  @Override
  public @NonNull HttpResponse header(@NonNull String name, @NonNull String value) {
    this.httpResponse.headers().set(name, value);
    return this;
  }

  @Override
  public @NonNull HttpResponse removeHeader(@NonNull String name) {
    this.httpResponse.headers().remove(name);
    return this;
  }

  @Override
  public @NonNull HttpResponse clearHeaders() {
    this.httpResponse.headers().clear();
    return this;
  }

  @Override
  public boolean hasHeader(@NonNull String name) {
    return this.httpResponse.headers().contains(name);
  }

  @Override
  public @NonNull Map<String, String> headers() {
    Map<String, String> maps = new HashMap<>(this.httpResponse.headers().size());

    for (var key : this.httpResponse.headers().names()) {
      maps.put(key, this.httpResponse.headers().get(key));
    }

    return maps;
  }

  @Override
  public @NonNull HttpVersion version() {
    return super.versionFromNetty(this.httpResponse.protocolVersion());
  }

  @Override
  public @NonNull HttpResponse version(@NonNull HttpVersion version) {
    this.httpResponse.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  @Override
  public byte[] body() {
    return this.httpResponse.content().array();
  }

  @Override
  public @NonNull String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  @Override
  public @NonNull HttpResponse body(byte[] byteArray) {
    this.httpResponse.content().clear();
    this.httpResponse.content().writeBytes(byteArray);
    return this;
  }

  @Override
  public @NonNull HttpResponse body(@NonNull String text) {
    this.httpResponse.content().clear();
    this.httpResponse.content().writeBytes(text.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  @Override
  public InputStream bodyStream() {
    return this.responseInputStream;
  }

  @Override
  public @NonNull HttpResponse body(InputStream body) {
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
