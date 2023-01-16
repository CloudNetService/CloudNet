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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;

/**
 * A listener which can be registered to a web socket channel.
 *
 * @see WebSocketChannel#addListener(WebSocketListener...)
 * @since 4.0
 */
@FunctionalInterface
public interface WebSocketListener {

  /**
   * Handles each received web socket frame. For each frame each registered listener to a web socket channel is called.
   * A response can be sent through the methods provided for that purpose in the given web socket channel.
   * <p>
   * This method is not called when a client indicates that the connection to will be closed. For that purpose listen to
   * the {@link #handleClose(WebSocketChannel, AtomicInteger, AtomicReference)} method instead.
   *
   * @param channel the channel to which the frame was sent.
   * @param type    the type of frame sent by the client.
   * @param bytes   the frame content which was transferred.
   * @throws Exception            if any exception occurs during handling of the frame.
   * @throws NullPointerException if either the given channel or type is null.
   */
  void handle(@NonNull WebSocketChannel channel, @NonNull WebSocketFrameType type, byte[] bytes) throws Exception;

  /**
   * Handles the close frame either send to the component or sent by the component. Changes to the status code and
   * status text reference will be reflected to all following listeners and the final close frame sent to the client.
   * <p>
   * If this method changes the status code of the close, the new status code must be a valid code as defined in
   * <a href="https://tools.ietf.org/html/rfc6455#section-7.4">RFC 6455</a>, else sending the frame to the recipient
   * will result in an exception.
   *
   * @param channel    the channel from which the frame came / to which the frame will be sent.
   * @param statusCode a reference to the status code of the frame, must be set to valid one when changing.
   * @param reasonText the reason text of the close, might evaluate to null (no reason).
   * @throws NullPointerException if either the given channel, status code or reason text reference is null.
   */
  default void handleClose(
    @NonNull WebSocketChannel channel,
    @NonNull AtomicInteger statusCode,
    @NonNull AtomicReference<String> reasonText
  ) {

  }
}
