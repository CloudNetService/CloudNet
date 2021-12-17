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

import java.io.InputStream;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface IHttpMessage<T extends IHttpMessage<?>> {

  @NotNull IHttpContext context();

  @Nullable String header(@NotNull String name);

  int headerAsInt(@NotNull String name);

  boolean headerAsBoolean(@NotNull String name);

  @NotNull T header(@NotNull String name, @NotNull String value);

  @NotNull T removeHeader(@NotNull String name);

  @NotNull T clearHeaders();

  boolean hasHeader(@NotNull String name);

  @NotNull Map<String, String> headers();

  @NotNull HttpVersion version();

  @NotNull T version(@NotNull HttpVersion version);

  byte[] body();

  @NotNull String bodyAsString();

  @NotNull T body(byte[] byteArray);

  @NotNull T body(@NotNull String text);

  @UnknownNullability InputStream bodyStream();

  @NotNull T body(@Nullable InputStream body);

  boolean hasBody();
}
