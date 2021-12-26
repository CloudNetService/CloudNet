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

package de.dytanic.cloudnet.driver.network.http.websocket;

import de.dytanic.cloudnet.driver.network.http.HttpChannel;
import java.util.Collection;
import lombok.NonNull;

public interface WebSocketChannel extends AutoCloseable {

  @NonNull WebSocketChannel addListener(@NonNull WebSocketListener... listeners);

  @NonNull WebSocketChannel removeListener(@NonNull WebSocketListener... listeners);

  @NonNull WebSocketChannel removeListener(@NonNull Collection<Class<? extends WebSocketListener>> classes);

  @NonNull WebSocketChannel removeListener(@NonNull ClassLoader classLoader);

  @NonNull WebSocketChannel clearListeners();

  @NonNull Collection<WebSocketListener> listeners();

  @NonNull WebSocketChannel sendWebSocketFrame(@NonNull WebSocketFrameType webSocketFrameType, @NonNull String text);

  @NonNull WebSocketChannel sendWebSocketFrame(@NonNull WebSocketFrameType webSocketFrameType, byte[] bytes);

  void close(int statusCode, @NonNull String reasonText);

  @NonNull HttpChannel channel();
}
