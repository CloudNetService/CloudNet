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

package eu.cloudnetservice.driver.network.http.annotation.parser;

import eu.cloudnetservice.driver.network.http.HttpHandleException;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import lombok.NonNull;

/**
 * A stackless exception which can be thrown when processing a http handler annotation.
 *
 * @since 4.0
 */
public final class AnnotationHttpHandleException extends HttpHandleException {

  /**
   * Constructs a new AnnotationHttpHandleException instance.
   *
   * @param path   the path to which the request was sent during which the handling error occurred.
   * @param reason the reason why the error occurred.
   * @throws NullPointerException if the given path or reason is null.
   */
  public AnnotationHttpHandleException(@NonNull String path, @NonNull String reason) {
    this(path, reason, HttpResponseCode.BAD_REQUEST, null);
  }

  /**
   * Constructs a new AnnotationHttpHandleException instance.
   *
   * @param path         the path to which the request was sent during which the handling error occurred.
   * @param reason       the reason why the error occurred.
   * @param responseCode the http status code to set in the response.
   * @param responseBody the response body to set.
   * @throws NullPointerException if the given path, reason or status is null.
   */
  public AnnotationHttpHandleException(
    @NonNull String path,
    @NonNull String reason,
    @NonNull HttpResponseCode responseCode,
    byte[] responseBody
  ) {
    super(responseCode, responseBody, String.format("Unable to handle http request on path \"%s\": %s", path, reason));
  }
}
