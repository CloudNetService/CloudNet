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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface IHttpContext {

  @Nullable IWebSocketChannel upgrade();

  @Nullable IWebSocketChannel webSocketChanel();

  @NonNull IHttpChannel channel();

  @NonNull IHttpRequest request();

  @NonNull IHttpResponse response();

  @Nullable IHttpHandler peekLast();

  boolean cancelNext();

  @NonNull IHttpContext cancelNext(boolean cancelNext);

  @NonNull IHttpComponent<?> component();

  @NonNull IHttpContext closeAfter(boolean value);

  boolean closeAfter();

  @Nullable HttpCookie cookie(@NonNull String name);

  @NonNull Collection<HttpCookie> cookies();

  boolean hasCookie(@NonNull String name);

  @NonNull IHttpContext cookies(@NonNull Collection<HttpCookie> cookies);

  @NonNull IHttpContext addCookie(@NonNull HttpCookie httpCookie);

  @NonNull IHttpContext removeCookie(@NonNull String name);

  @NonNull IHttpContext clearCookies();

  @NonNull String pathPrefix();
}
