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

import lombok.NonNull;

/**
 * Represents a response http message transferred from a server to a client.
 *
 * @since 4.0
 */
public interface HttpResponse extends HttpMessage<HttpResponse> {

  /**
   * The status code of the http response. The response status code set here should, but must not be a valid one. See
   * the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">mdn</a> documentation about status codes for
   * more information.
   *
   * @return the http response code of this response.
   */
  @NonNull HttpResponseCode status();

  /**
   * Sets the status of this response.
   *
   * @param code the new response status.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given response code is null.
   */
  @NonNull HttpResponse status(@NonNull HttpResponseCode code);
}
