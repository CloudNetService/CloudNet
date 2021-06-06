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
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyHttpServerRequest extends NettyHttpMessage implements IHttpRequest {

  protected final NettyHttpServerContext context;

  protected final HttpRequest httpRequest;

  protected final URI uri;

  protected final Map<String, String> pathParameters;

  protected final Map<String, List<String>> queryParameters;

  protected byte[] body;

  public NettyHttpServerRequest(NettyHttpServerContext context, HttpRequest httpRequest,
    Map<String, String> pathParameters, URI uri) {
    this.context = context;
    this.httpRequest = httpRequest;
    this.uri = uri;
    this.pathParameters = pathParameters;
    this.queryParameters = new QueryStringDecoder(httpRequest.uri()).parameters();
  }

  @Override
  public Map<String, String> pathParameters() {
    return this.pathParameters;
  }

  @Override
  public String path() {
    return this.uri.getPath();
  }

  @Override
  public String uri() {
    return this.httpRequest.uri();
  }

  @Override
  public String method() {
    return this.httpRequest.method().name();
  }

  @Override
  public Map<String, List<String>> queryParameters() {
    return this.queryParameters;
  }

  @Override
  public IHttpContext context() {
    return this.context;
  }

  @Override
  public String header(String name) {
    Preconditions.checkNotNull(name);
    return this.httpRequest.headers().getAsString(name);
  }

  @Override
  public int headerAsInt(String name) {
    Preconditions.checkNotNull(name);
    return this.httpRequest.headers().getInt(name);
  }

  @Override
  public boolean headerAsBoolean(String name) {
    Preconditions.checkNotNull(name);
    return Boolean.parseBoolean(this.httpRequest.headers().get(name));
  }

  @Override
  public IHttpRequest header(String name, String value) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(value);

    this.httpRequest.headers().set(name, value);
    return this;
  }

  @Override
  public IHttpRequest removeHeader(String name) {
    Preconditions.checkNotNull(name);
    this.httpRequest.headers().remove(name);
    return this;
  }

  @Override
  public IHttpRequest clearHeaders() {
    this.httpRequest.headers().clear();
    return this;
  }

  @Override
  public boolean hasHeader(String name) {
    Preconditions.checkNotNull(name);
    return this.httpRequest.headers().contains(name);
  }

  @Override
  public Map<String, String> headers() {
    Map<String, String> maps = new HashMap<>(this.httpRequest.headers().size());

    for (String key : this.httpRequest.headers().names()) {
      maps.put(key, this.httpRequest.headers().get(key));
    }

    return maps;
  }

  @Override
  public HttpVersion version() {
    return super.getCloudNetHttpVersion(this.httpRequest.protocolVersion());
  }

  @Override
  public IHttpRequest version(HttpVersion version) {
    Preconditions.checkNotNull(version);

    this.httpRequest.setProtocolVersion(super.getNettyHttpVersion(version));
    return this;
  }

  @Override
  public byte[] body() {
    if (this.httpRequest instanceof FullHttpRequest) {
      if (this.body == null) {
        FullHttpRequest httpRequest = (FullHttpRequest) this.httpRequest;

        int length = httpRequest.content().readableBytes();

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
  public String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  @Override
  public IHttpRequest body(byte[] byteArray) {
    throw new UnsupportedOperationException("No setting http body in request message by client");
  }

  @Override
  public IHttpRequest body(String text) {
    Preconditions.checkNotNull(text);

    return this.body(text.getBytes(StandardCharsets.UTF_8));
  }

}
