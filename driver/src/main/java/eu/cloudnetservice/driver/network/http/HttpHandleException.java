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

package eu.cloudnetservice.driver.network.http;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An exception which can be thrown by a handler to set the status and body of the response for the current http
 * request. Throwing this exception will not prevent other handlers from processing the request as well.
 *
 * @since 4.0
 */
public class HttpHandleException extends RuntimeException {

  private final byte[] responseBody;
  private final HttpResponseCode responseCode;

  /**
   * Constructs a new HttpHandleException instance.
   *
   * @param responseCode the http status code to set in the response.
   * @param responseBody the response body to set, null to reset the body.
   * @param message      the detail message why the exception occurred, for debugging only.
   * @throws NullPointerException if the given response code or message is null.
   */
  public HttpHandleException(@NonNull HttpResponseCode responseCode, byte[] responseBody, @NonNull String message) {
    super(message);
    this.responseBody = responseBody;
    this.responseCode = responseCode;
  }

  /**
   * Get the body to set in the response, null to reset the body.
   *
   * @return the body to set in the response to the current processing request.
   */
  public byte[] responseBody() {
    return this.responseBody;
  }

  /**
   * Get the status code to set in the response to the current processing request.
   *
   * @return the status code to set in the response.
   */
  public @NonNull HttpResponseCode responseCode() {
    return this.responseCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Throwable fillInStackTrace() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Throwable initCause(@Nullable Throwable cause) {
    return this;
  }
}
