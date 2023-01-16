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

package eu.cloudnetservice.driver.network.http.websocket;

/**
 * All types of web socket frames which are valid to send to a web socket channel. All of these types are defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc6455">RFC 6455</a> section 5.5 and 5.6.
 *
 * @since 4.0
 */
public enum WebSocketFrameType {

  /**
   * A frame to check if the recipient is still alive. The recipient must answer with a pong frame if no close frame was
   * received previously, returning the same application data as sent in the ping. For more details see
   * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.2">RFC-6455, section 5.5.2</a>.
   */
  PING,
  /**
   * A frame which is a response to a ping. The body of the frame must contain the same data as supplied in the ping.
   * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.3">RFC-6455, section 5.5.3</a> for more
   * details.
   */
  PONG,
  /**
   * A frame which holds text in an utf-8 format in its body. For more information see
   * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.6">RFC-6455, section 5.6</a>.
   */
  TEXT,
  /**
   * A frame which indicates the recipient that the connection will be closed, optionally containing a reason text. See
   * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1">RFC-6455, section 5.5.1</a> for more
   * details.
   */
  CLOSE,
  /**
   * A frame which holds raw binary data in its body, the interpretation of the data is up to the application. For more
   * details see <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.6">RFC-6455, section 5.6</a>.
   */
  BINARY
}
