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

import java.io.InputStream;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface HttpMessage<T extends HttpMessage<?>> {

  @NonNull HttpContext context();

  @Nullable String header(@NonNull String name);

  int headerAsInt(@NonNull String name);

  boolean headerAsBoolean(@NonNull String name);

  @NonNull T header(@NonNull String name, @NonNull String value);

  @NonNull T removeHeader(@NonNull String name);

  @NonNull T clearHeaders();

  boolean hasHeader(@NonNull String name);

  @NonNull Map<String, String> headers();

  @NonNull HttpVersion version();

  @NonNull T version(@NonNull HttpVersion version);

  byte[] body();

  @NonNull String bodyAsString();

  @NonNull T body(byte[] byteArray);

  @NonNull T body(@NonNull String text);

  @UnknownNullability InputStream bodyStream();

  @NonNull T body(@Nullable InputStream body);

  boolean hasBody();
}
