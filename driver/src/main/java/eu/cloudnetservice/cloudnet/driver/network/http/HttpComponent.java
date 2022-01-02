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

import java.util.Collection;
import lombok.NonNull;

public interface HttpComponent<T extends HttpComponent<?>> extends AutoCloseable {

  boolean sslEnabled();

  @NonNull T registerHandler(@NonNull String path, @NonNull HttpHandler... handlers);

  @NonNull T registerHandler(@NonNull String path, int priority, @NonNull HttpHandler... handlers);

  @NonNull T registerHandler(@NonNull String path, Integer port, int priority, @NonNull HttpHandler... handlers);

  @NonNull T removeHandler(@NonNull HttpHandler handler);

  @NonNull T removeHandler(@NonNull Class<? extends HttpHandler> handler);

  @NonNull T removeHandler(@NonNull ClassLoader classLoader);

  @NonNull Collection<HttpHandler> httpHandlers();

  @NonNull T clearHandlers();
}
