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
import eu.cloudnetservice.driver.network.http.HttpRequest;
import eu.cloudnetservice.driver.network.http.HttpVersion;
import io.netty5.buffer.BufferInputStream;
import io.netty5.handler.codec.http.FullHttpRequest;
import io.netty5.handler.codec.http.QueryStringDecoder;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of a http request.
 *
 * @since 4.0
 */
final class NettyHttpServerRequest extends NettyHttpMessage implements HttpRequest {

  private final NettyHttpServerContext context;

  private final URI uri;
  private final io.netty5.handler.codec.http.HttpRequest httpRequest;

  private final Map<String, String> pathParameters;
  private final Map<String, List<String>> queryParameters;

  private byte[] body;

  /**
   * Constructs a new netty http request instance.
   *
   * @param context        the context in which the request is processed.
   * @param httpRequest    the original netty request which gets wrapped.
   * @param pathParameters the extracted path parameters from the uri.
   * @param uri            the original uri of the request.
   * @throws NullPointerException if one of the given properties is null.
   */
  public NettyHttpServerRequest(
    @NonNull NettyHttpServerContext context,
    @NonNull io.netty5.handler.codec.http.HttpRequest httpRequest,
    @NonNull Map<String, String> pathParameters,
    @NonNull URI uri
  ) {
    this.context = context;
    this.httpRequest = httpRequest;
    this.uri = uri;
    this.pathParameters = pathParameters;
    this.queryParameters = new QueryStringDecoder(httpRequest.uri()).parameters();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> pathParameters() {
    return this.pathParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String path() {
    return this.uri.getPath();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String uri() {
    return this.httpRequest.uri();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String method() {
    return this.httpRequest.method().name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, List<String>> queryParameters() {
    return this.queryParameters;
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
    var headerValue = this.httpRequest.headers().get(name);
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
  public @NonNull HttpRequest header(@NonNull String name, @NonNull String value) {
    this.httpRequest.headers().set(name, value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest removeHeader(@NonNull String name) {
    this.httpRequest.headers().remove(name);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest clearHeaders() {
    this.httpRequest.headers().clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasHeader(@NonNull String name) {
    return this.httpRequest.headers().contains(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> headers() {
    // init the target map to copy all headers into - that target map is immune to later resizes
    var headers = this.httpRequest.headers();
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
    return super.versionFromNetty(this.httpRequest.protocolVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest version(@NonNull HttpVersion version) {
    this.httpRequest.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] body() {
    if (this.httpRequest instanceof FullHttpRequest request) {
      if (this.body == null) {
        // initialize the body
        var length = request.payload().readableBytes();
        this.body = new byte[length];

        // copy out the bytes of the buffer
        request.payload().copyInto(request.payload().readerOffset(), this.body, 0, length);
      }

      return this.body;
    }

    return new byte[0];
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
  public @NonNull HttpRequest body(byte[] byteArray) {
    throw new UnsupportedOperationException("Unable to set body in request");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest body(@NonNull String text) {
    return this.body(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream bodyStream() {
    if (this.httpRequest instanceof FullHttpRequest fullHttpRequest) {
      return new BufferInputStream(fullHttpRequest.payload().send());
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest body(@Nullable InputStream body) {
    throw new UnsupportedOperationException("Unable to set body in request");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasBody() {
    return this.httpRequest instanceof FullHttpRequest request && request.payload().readableBytes() > 0;
  }
}
