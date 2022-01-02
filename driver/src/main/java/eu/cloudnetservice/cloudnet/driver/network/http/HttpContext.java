/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.http;

import eu.cloudnetservice.cloudnet.driver.network.http.websocket.WebSocketChannel;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface HttpContext {

  @Nullable WebSocketChannel upgrade();

  @Nullable WebSocketChannel webSocketChanel();

  @NonNull HttpChannel channel();

  @NonNull HttpRequest request();

  @NonNull HttpResponse response();

  @Nullable HttpHandler peekLast();

  boolean cancelNext();

  @NonNull HttpContext cancelNext(boolean cancelNext);

  @NonNull HttpComponent<?> component();

  @NonNull HttpContext closeAfter(boolean value);

  boolean closeAfter();

  @Nullable HttpCookie cookie(@NonNull String name);

  @NonNull Collection<HttpCookie> cookies();

  boolean hasCookie(@NonNull String name);

  @NonNull HttpContext cookies(@NonNull Collection<HttpCookie> cookies);

  @NonNull HttpContext addCookie(@NonNull HttpCookie httpCookie);

  @NonNull HttpContext removeCookie(@NonNull String name);

  @NonNull HttpContext clearCookies();

  @NonNull String pathPrefix();
}
