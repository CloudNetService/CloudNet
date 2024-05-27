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

import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Represents a http message which got sent from a client to the server.
 *
 * @since 4.0
 */
public interface HttpRequest extends HttpMessage<HttpRequest> {

  /**
   * The path parameters of the request, parsed before processing the next handler in the chain. A handler can provide a
   * path parameter by wrapping its name into {name} would result as a key in this map at the given path index. For
   * example the path {@code /docs/{topic}/index/{page}} would contain the path parameters topic and page, parsed from
   * the request uri before calling the request handler. Each handler can access the path parameters of handlers
   * beforehand, however duplicate path parameter names will override each other.
   *
   * @return all parsed path parameters for the current request.
   */
  @NonNull Map<String, String> pathParameters();

  /**
   * Get the full, originally requested path.
   *
   * @return the full, originally requested path.
   */
  @NonNull String path();

  /**
   * Get the full requested uri.
   *
   * @return the full requested uri.
   */
  @NonNull String uri();

  /**
   * Get the method used for this request. The method might be one of
   * <ul>
   *   <li>OPTIONS
   *   <li>GET
   *   <li>HEAD
   *   <li>POST
   *   <li>PUT
   *   <li>PATCH
   *   <li>DELETE
   *   <li>TRACE
   *   <li>CONNECT
   * </ul>
   * <p>
   * See the <a href="https://developer.mozilla.org/de/docs/Web/HTTP/Methods">mdn</a> documentation for more information
   * about http request methods.
   *
   * @return the request method of the request.
   */
  @NonNull String method();

  /**
   * Get all query parameters mapped by the key of it to the value. Each query parameter can have multiple values set.
   * The maximum amount of query parameters which will get decoded for a request are 1024.
   *
   * @return the query parameters supplied in the request uri.
   */
  @NonNull Map<String, List<String>> queryParameters();
}
