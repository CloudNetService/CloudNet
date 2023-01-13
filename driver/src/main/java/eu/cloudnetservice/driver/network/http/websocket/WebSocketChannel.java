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

import eu.cloudnetservice.driver.network.http.HttpChannel;
import eu.cloudnetservice.driver.network.http.HttpContext;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A web socket channel to which can be listened and written to after upgrading a http connection.
 *
 * @see HttpContext#upgrade()
 * @since 4.0
 */
public interface WebSocketChannel extends AutoCloseable {

  /**
   * Adds the given listeners to this web socket channel. Each listener will be called when a web socket frame is
   * received from the wrapped channel.
   *
   * @param listeners the listeners to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given listeners are null.
   */
  @NonNull WebSocketChannel addListener(@NonNull WebSocketListener... listeners);

  /**
   * Removes the given listeners from the channel if they were added previously.
   *
   * @param listeners the listeners to remove.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given listeners are null.
   */
  @NonNull WebSocketChannel removeListener(@NonNull WebSocketListener... listeners);

  /**
   * Removes all listeners from this channel whose classes were loaded by the given class loader.
   *
   * @param classLoader the class loader to remove the listener of.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  @NonNull WebSocketChannel removeListener(@NonNull ClassLoader classLoader);

  /**
   * Removes all previously registered listeners from this channel.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  @NonNull WebSocketChannel clearListeners();

  /**
   * Get all listeners which were registered to this channel previously.
   *
   * @return all registered listeners on this channel.
   */
  @NonNull Collection<WebSocketListener> listeners();

  /**
   * Sends a web socket frame of the given type into this channel. This method sends frames of the type PING, PONG and
   * TEXT. Any other type will be silently converted to a binary frame holding the given text in its body. This method
   * call is equivalent to {@code channel.sendWebSocketFrame(type, text.getBytes(StandardCharsets.UTF_8))}.
   *
   * @param webSocketFrameType the type of web socket frame to send.
   * @param text               the string content of the frame.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if either the given type or text is null.
   */
  @NonNull WebSocketChannel sendWebSocketFrame(@NonNull WebSocketFrameType webSocketFrameType, @NonNull String text);

  /**
   * Sends a web socket frame of the given type into this channel. This method sends frames of the type PING, PONG and
   * TEXT. Any other type will be silently converted to a binary frame holding the given bytes in its body. To send a
   * close frame use {@link #close(int, String)} instead.
   *
   * @param webSocketFrameType the type of web socket frame to send.
   * @param bytes              the bytes to put into the frame body.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given type is null.
   */
  @NonNull WebSocketChannel sendWebSocketFrame(@NonNull WebSocketFrameType webSocketFrameType, byte[] bytes);

  /**
   * Sends a close frame into this channel and closes the connection to the recipient without waiting for any kind of
   * response. Any listener added to this channel can change the given reason status code and the reason text. The given
   * status code must be valid as defined in <a href="https://tools.ietf.org/html/rfc6455#section-7.4">RFC 6455</a>.
   *
   * @param statusCode the status code of the close, must be valid as per RFC-6455.
   * @param reasonText the reason text for the close, no text indicates no special reason.
   * @throws IllegalArgumentException if an invalid status code was provided.
   */
  void close(int statusCode, @Nullable String reasonText);

  /**
   * Get the underlying channel which was upgraded to this web socket channel.
   *
   * @return the original, now upgraded channel.
   */
  @NonNull HttpChannel channel();
}
