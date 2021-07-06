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

import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import java.util.Collection;

public interface IWebSocketChannel extends AutoCloseable {

  IWebSocketChannel addListener(IWebSocketListener... listeners);

  IWebSocketChannel removeListener(IWebSocketListener... listeners);

  IWebSocketChannel removeListener(Collection<Class<? extends IWebSocketListener>> classes);

  IWebSocketChannel removeListener(ClassLoader classLoader);

  IWebSocketChannel clearListeners();

  Collection<IWebSocketListener> getListeners();

  IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, String text);

  IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, byte[] bytes);

  void close(int statusCode, String reasonText);

  IHttpChannel channel();

}
