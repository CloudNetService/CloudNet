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

import de.dytanic.cloudnet.common.INameable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class HttpCookie implements INameable {

  protected final String name;
  protected final String value;
  protected final String domain;
  protected final String path;

  protected final boolean httpOnly;
  protected final boolean secure;
  protected final boolean wrap;

  protected final long maxAge;

  public HttpCookie(@NotNull String name, @NotNull String value) {
    this(name, value, null, null, Long.MAX_VALUE);
  }

  public HttpCookie(
    @NotNull String name,
    @NotNull String value,
    @Nullable String domain,
    @Nullable String path,
    long maxAge
  ) {
    this(name, value, domain, path, false, false, false, maxAge);
  }

  public HttpCookie(
    @NotNull String name,
    @NotNull String value,
    @Nullable String domain,
    @Nullable String path,
    boolean httpOnly,
    boolean secure,
    boolean wrap,
    long maxAge
  ) {
    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.httpOnly = httpOnly;
    this.secure = secure;
    this.wrap = wrap;
    this.maxAge = maxAge;
  }

  @Override
  public @NotNull String name() {
    return this.name;
  }

  public @NotNull String value() {
    return this.value;
  }

  public @Nullable String domain() {
    return this.domain;
  }

  public @Nullable String path() {
    return this.path;
  }

  public long maxAge() {
    return this.maxAge;
  }

  public boolean httpOnly() {
    return this.httpOnly;
  }

  public boolean secure() {
    return this.secure;
  }

  public boolean wrap() {
    return this.wrap;
  }
}
