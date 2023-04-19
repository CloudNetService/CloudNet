/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.http;

import eu.cloudnetservice.common.Named;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cookie which can be sent by a client to the server using the {@code Cookie} header, and vise-vera using
 * the {@code Set-Cookie} header. See the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies">mdn</a>
 * documentation about cookies for more details.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class HttpCookie implements Named {

  protected final String name;
  protected final String value;
  protected final String domain;
  protected final String path;

  protected final boolean httpOnly;
  protected final boolean secure;
  protected final boolean wrap;

  protected final long maxAge;

  /**
   * Constructs a new http cookie instance. This constructor only takes the required values of a cookie.
   *
   * @param name  the name of the cookie.
   * @param value the value of the cookie.
   * @throws NullPointerException if either the given name or value is null.
   */
  public HttpCookie(@NonNull String name, @NonNull String value) {
    this(name, value, null, null, Long.MAX_VALUE);
  }

  /**
   * Constructs a new http cookie instance.
   *
   * @param name   the name of the cookie.
   * @param value  the value of the cookie.
   * @param domain the domain which is allowed to access the cookie. If not set it defaults to the current domain,
   *               excluding subdomains.
   * @param path   the path that must request in the request uri in order to include this cookie in the header.
   * @param maxAge the maximum age until the cookie gets deleted from the client.
   * @throws NullPointerException if either the given name or value is null.
   */
  public HttpCookie(
    @NonNull String name,
    @NonNull String value,
    @Nullable String domain,
    @Nullable String path,
    long maxAge
  ) {
    this(name, value, domain, path, false, false, false, maxAge);
  }

  /**
   * Constructs a new http cookie instance.
   *
   * @param name     the name of the cookie.
   * @param value    the value of the cookie.
   * @param domain   the domain which is allowed to access the cookie. If not set it defaults to the current domain,
   *                 excluding subdomains.
   * @param path     the path that must request in the request uri in order to include this cookie in the header.
   * @param httpOnly sets that the cookie is invisible to scripts and will only be sent in http requests to the server.
   * @param secure   sets that the cookie is only sent to the server when the connection is encrypted (using https).
   * @param wrap     sets if the value of this cookie should be wrapped in double quotes.
   * @param maxAge   the maximum age until the cookie gets deleted from the client.
   * @throws NullPointerException if either the given name or value is null.
   */
  public HttpCookie(
    @NonNull String name,
    @NonNull String value,
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

  /**
   * Get the name of this cookie.
   *
   * @return the name of this cookie.
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the value of this cookie.
   *
   * @return the value of this cookie.
   */
  public @NonNull String value() {
    return this.value;
  }

  /**
   * Get the domain which is allowed to receive the cookie. If not set only the current domain is allowed to receive the
   * cookie, excluding subdomains. If set to the current domain then subdomains are always included.
   *
   * @return the domain which is allowed to receive the cookie.
   */
  public @Nullable String domain() {
    return this.domain;
  }

  /**
   * Get the path which must be present in the request uri to send the cookie to the server. For example, if the path is
   * set to {@code /docs} then the following uris would receive the cookie:
   * <ol>
   *   <li>/docs
   *   <li>/docs/
   *   <li>/docs/getting-started
   * </ol>
   * while these would not receive the cookie:
   * <ol>
   *   <li>/
   *   <li>/helpdesk
   *   <li>/documentation/getting-started
   * </ol>
   *
   * @return the path which must be present in the request uri to send the cookie to the server.
   */
  public @Nullable String path() {
    return this.path;
  }

  /**
   * Get the max age this cookie persists on the client side until it gets emitted.
   *
   * @return the max age of this cookie.
   */
  public long maxAge() {
    return this.maxAge;
  }

  /**
   * Get if the cookie is only visible to http request, blocking access to the cookie from e.g. scripts running in the
   * browser.
   *
   * @return if the cookie is only visible to http request.
   */
  public boolean httpOnly() {
    return this.httpOnly;
  }

  /**
   * Get if this cookie should only get transferred to the server if the server connection is encrypted (using https).
   * The only point when it is transmitted to the server even when using http is to localhost.
   *
   * @return if this cookie should only get transferred to the server if the server connection is encrypted.
   */
  public boolean secure() {
    return this.secure;
  }

  /**
   * Get if the value of this cookie is wrapped in double quotes. This is an en-/de- coder only option which is not
   * visible on the client side.
   *
   * @return if the value of this cookie is wrapped in double quotes
   */
  public boolean wrap() {
    return this.wrap;
  }
}
