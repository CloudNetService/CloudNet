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

package eu.cloudnetservice.driver.network.http;

import lombok.NonNull;

/**
 * A http handler which allows listening to only specific http methods instead of all, or handling requests based on
 * their http method.
 *
 * @since 4.0
 */
public abstract class MethodHttpHandler extends HttpHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(@NonNull String path, @NonNull HttpContext context) throws Exception {
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

  /**
   * Handles a post http request whose path (and other supplied attributes) while registering is matching the requested
   * path of the client. A request is only processed by one handler at a time, giving the handler full control about
   * changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handlePost(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a get http request whose path (and other supplied attributes) while registering is matching the requested
   * path of the client. A request is only processed by one handler at a time, giving the handler full control about
   * changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handleGet(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a put http request whose path (and other supplied attributes) while registering is matching the requested
   * path of the client. A request is only processed by one handler at a time, giving the handler full control about
   * changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handlePut(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a head http request whose path (and other supplied attributes) while registering is matching the requested
   * path of the client. A request is only processed by one handler at a time, giving the handler full control about
   * changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handleHead(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a delete http request whose path (and other supplied attributes) while registering is matching the
   * requested path of the client. A request is only processed by one handler at a time, giving the handler full control
   * about changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handleDelete(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a patch http request whose path (and other supplied attributes) while registering is matching the requested
   * path of the client. A request is only processed by one handler at a time, giving the handler full control about
   * changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handlePatch(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a trace http request whose path (and other supplied attributes) while registering is matching the requested
   * path of the client. A request is only processed by one handler at a time, giving the handler full control about
   * changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handleTrace(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles an options http request whose path (and other supplied attributes) while registering is matching the
   * requested path of the client. A request is only processed by one handler at a time, giving the handler full control
   * about changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handleOptions(@NonNull String path, @NonNull HttpContext context) throws Exception;

  /**
   * Handles a connect http request whose path (and other supplied attributes) while registering is matching the
   * requested path of the client. A request is only processed by one handler at a time, giving the handler full control
   * about changing the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Exception            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handleConnect(@NonNull String path, @NonNull HttpContext context) throws Exception;
}
