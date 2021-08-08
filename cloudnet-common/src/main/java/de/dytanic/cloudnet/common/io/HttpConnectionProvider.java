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

package de.dytanic.cloudnet.common.io;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a {@link HttpURLConnection} with some common settings
 */
public final class HttpConnectionProvider {

  public static final Map<String, String> DEFAULT_REQUEST_PROPERTIES = ImmutableMap.of(
    "User-Agent",
    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
  );

  private HttpConnectionProvider() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull HttpURLConnection provideConnection(@NotNull String endpoint) {
    return provideConnection(endpoint, DEFAULT_REQUEST_PROPERTIES);
  }

  public static @NotNull HttpURLConnection provideConnection(@NotNull URL endpoint) {
    return provideConnection(endpoint, DEFAULT_REQUEST_PROPERTIES);
  }

  public static @NotNull HttpURLConnection provideConnection(
    @NotNull String endpoint,
    @NotNull Map<String, String> requestProperties
  ) {
    return provideConnection(endpoint, requestProperties, 5_000);
  }

  public static @NotNull HttpURLConnection provideConnection(
    @NotNull URL endpoint,
    @NotNull Map<String, String> requestProperties
  ) {
    return provideConnection(endpoint, requestProperties, 5_000);
  }

  public static @NotNull HttpURLConnection provideConnection(@NotNull String endpoint, int timeout) {
    return provideConnection(endpoint, DEFAULT_REQUEST_PROPERTIES, timeout);
  }

  public static @NotNull HttpURLConnection provideConnection(@NotNull URL endpoint, int timeout) {
    return provideConnection(endpoint, DEFAULT_REQUEST_PROPERTIES, timeout);
  }

  public static @NotNull HttpURLConnection provideConnection(
    @NotNull String endpoint,
    @NotNull Map<String, String> requestProperties,
    int timeout
  ) {
    try {
      return provideConnection(new URL(endpoint), requestProperties, timeout);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to provide http connection", exception);
    }
  }

  public static @NotNull HttpURLConnection provideConnection(
    @NotNull URL endpoint,
    @NotNull Map<String, String> requestProperties,
    int timeout
  ) {
    try {
      HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
      // timeout
      connection.setReadTimeout(timeout);
      connection.setConnectTimeout(timeout);
      // properties
      requestProperties.forEach(connection::setRequestProperty);
      return connection;
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to provide http connection", exception);
    }
  }
}
