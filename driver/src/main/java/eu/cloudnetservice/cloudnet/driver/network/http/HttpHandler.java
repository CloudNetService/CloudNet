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

package eu.cloudnetservice.cloudnet.driver.network.http;

import lombok.NonNull;

/**
 * A handler for a http request. Each request, matching the given attributes supplied when registering, will be called
 * directly into this handler. A request is only processed by one handler at a time. Handlers with a high priority will
 * get called before handlers with a low priority.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface HttpHandler {

  int PRIORITY_HIGH = 64;
  int PRIORITY_NORMAL = 32;
  int PRIORITY_LOW = 16;
  int PRIORITY_LOWEST = 0;

  /**
   * Handles a http request whose path (and other supplied attributes) while registering is matching the requested path
   * of the client. A request is only processed by one handler at a time, giving the handler full control about changing
   * the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  void handle(@NonNull String path, @NonNull HttpContext context) throws Exception;
}
