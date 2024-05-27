/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

final class UriBuilder {

  private final List<String> queryParameters = new LinkedList<>();
  private final StringJoiner pathBuilder = new StringJoiner("/", "/", "");

  private String scheme = "http";
  private String host = "127.0.0.1";
  private int port = 80;
  private String fragment;

  public static UriBuilder create() {
    return new UriBuilder();
  }

  public UriBuilder scheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  public UriBuilder host(String host) {
    this.host = host;
    return this;
  }

  public UriBuilder port(int port) {
    this.port = port;
    return this;
  }

  public UriBuilder fragment(String fragment) {
    this.fragment = fragment;
    return this;
  }

  public UriBuilder path(String... pathParts) {
    for (String pathPart : pathParts) {
      if (pathPart != null && !pathPart.isBlank()) {
        this.pathBuilder.add(pathPart);
      }
    }

    return this;
  }

  public UriBuilder addQueryParameter(String name) {
    this.queryParameters.add(name);
    return this;
  }

  public UriBuilder addQueryParameter(String name, String value) {
    this.queryParameters.add(String.format("%s=%s", name, value));
    return this;
  }

  public URI build() {
    try {
      var fullPath = this.pathBuilder.toString();
      var fullQuery = String.join("&", this.queryParameters);
      return new URI(this.scheme, null, this.host, this.port, fullPath, fullQuery, this.fragment);
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Unable to build URI from given parameters", exception);
    }
  }
}
