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
import org.jetbrains.annotations.Nullable;

public interface IHttpMessage<T extends IHttpMessage<?>> {

  IHttpContext context();

  String header(String name);

  int headerAsInt(String name);

  boolean headerAsBoolean(String name);

  T header(String name, String value);

  T removeHeader(String name);

  T clearHeaders();

  boolean hasHeader(String name);

  Map<String, String> headers();

  HttpVersion version();

  T version(HttpVersion version);

  byte[] body();

  String bodyAsString();

  T body(byte[] byteArray);

  T body(String text);

  @Nullable
  InputStream bodyStream();

  T body(@Nullable InputStream body);

  boolean hasBody();
}
