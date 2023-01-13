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

import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponse;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.HttpVersion;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.handler.codec.http.DefaultFullHttpResponse;
import io.netty5.handler.codec.http.FullHttpResponse;
import io.netty5.handler.codec.http.HttpRequest;
import io.netty5.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of a http response.
 *
 * @since 4.0
 */
final class NettyHttpServerResponse extends NettyHttpMessage implements HttpResponse {

  final FullHttpResponse httpResponse;
  private final NettyHttpServerContext context;

  private InputStream responseInputStream;

  /**
   * Constructs a new netty http response instance.
   *
   * @param context     the context in which the request (and this response to the request) is handled.
   * @param httpRequest the original unwrapped request sent to the server.
   * @throws NullPointerException if either the context or request is null.
   */
  public NettyHttpServerResponse(@NonNull NettyHttpServerContext context, @NonNull HttpRequest httpRequest) {
    this.context = context;
    this.httpResponse = new DefaultFullHttpResponse(
      httpRequest.protocolVersion(),
      HttpResponseStatus.NOT_FOUND,
      DefaultBufferAllocators.offHeapAllocator().allocate(0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponseCode status() {
    return HttpResponseCode.fromNumeric(this.httpResponse.status().code());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse status(@NonNull HttpResponseCode code) {
    this.httpResponse.setStatus(HttpResponseStatus.valueOf(code.code()));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext context() {
    return this.context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable String header(@NonNull String name) {
    var headerValue = this.httpResponse.headers().get(name);
    return headerValue == null ? null : headerValue.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Integer headerAsInt(@NonNull String name) {
    var headerValue = this.header(name);
    return headerValue == null ? null : Ints.tryParse(headerValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean headerAsBoolean(@NonNull String name) {
    var headerValue = this.header(name);
    return Boolean.parseBoolean(headerValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse header(@NonNull String name, @NonNull String value) {
    this.httpResponse.headers().set(name, value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse removeHeader(@NonNull String name) {
    this.httpResponse.headers().remove(name);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse clearHeaders() {
    this.httpResponse.headers().clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasHeader(@NonNull String name) {
    return this.httpResponse.headers().contains(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> headers() {
    // init the target map to copy all headers into - that target map is immune to later resizes
    var headers = this.httpResponse.headers();
    Map<String, String> headerMap = new HashMap<>(headers.size() + 1, 1f);

    // copy over all headers
    for (var entry : headers) {
      headerMap.put(entry.getKey().toString(), entry.getValue().toString());
    }

    return headerMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpVersion version() {
    return super.versionFromNetty(this.httpResponse.protocolVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse version(@NonNull HttpVersion version) {
    this.httpResponse.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] body() {
    var payload = this.httpResponse.payload();

    // initialize the body
    var length = payload.readableBytes();
    var body = new byte[length];

    // copy out the bytes of the buffer
    payload.copyInto(payload.readerOffset(), body, 0, length);
    return body;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse body(byte[] byteArray) {
    this.httpResponse.payload()
      .resetOffsets()
      .fill((byte) 0)
      .writeBytes(byteArray);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse body(@NonNull String text) {
    return this.body(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream bodyStream() {
    return this.responseInputStream;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasBody() {
    return this.httpResponse.payload().readableBytes() > 0 || this.responseInputStream != null;
  }
}
