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

package de.dytanic.cloudnet.driver.network.http;

import lombok.NonNull;

public interface MethodHttpHandler extends HttpHandler {

  @Override
  default void handle(@NonNull String path, @NonNull HttpContext context) throws Exception {
    switch (context.request().method().toUpperCase()) {
      case "GET" -> this.handleGet(path, context);
      case "POST" -> this.handlePost(path, context);
      case "PATCH" -> this.handlePatch(path, context);
      case "PUT" -> this.handlePut(path, context);
      case "DELETE" -> this.handleDelete(path, context);
      case "HEAD" -> this.handleHead(path, context);
      case "TRACE" -> this.handleTrace(path, context);
      case "OPTIONS" -> this.handleOptions(path, context);
      case "CONNECT" -> this.handleConnect(path, context);
      default -> {
      }
    }
  }

  void handlePost(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handleGet(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handlePut(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handleHead(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handleDelete(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handlePatch(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handleTrace(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handleOptions(@NonNull String path, @NonNull HttpContext context) throws Exception;

  void handleConnect(@NonNull String path, @NonNull HttpContext context) throws Exception;
}
