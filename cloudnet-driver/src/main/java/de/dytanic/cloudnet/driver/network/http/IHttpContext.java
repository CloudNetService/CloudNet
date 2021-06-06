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

package de.dytanic.cloudnet.driver.network.http;

import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import java.util.Collection;

public interface IHttpContext {

  IWebSocketChannel upgrade();

  IWebSocketChannel webSocketChanel();

  IHttpChannel channel();

  IHttpRequest request();

  IHttpResponse response();

  IHttpHandler peekLast();

  boolean cancelNext();

  IHttpComponent<?> component();

  IHttpContext closeAfter(boolean value);

  boolean closeAfter();

  HttpCookie cookie(String name);

  Collection<HttpCookie> cookies();

  boolean hasCookie(String name);

  IHttpContext setCookies(Collection<HttpCookie> cookies);

  IHttpContext addCookie(HttpCookie httpCookie);

  IHttpContext removeCookie(String name);

  IHttpContext clearCookies();

}
