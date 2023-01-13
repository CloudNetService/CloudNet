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

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketChannel;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The context of a http request. Each request will pass this context to all handlers of it, allowing them the maximum
 * control about the request flow.
 *
 * @see HttpHandler
 * @since 4.0
 */
public interface HttpContext {

  /**
   * Upgrades this context to a websocket connection. In normal cases, as CloudNet currently only supports Http 1.X the
   * client sends an upgrade header in its request to signal that an upgrade to a websocket channel is requested. See
   * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Protocol_upgrade_mechanism">Mozilla - Upgrade</a> docs
   * for more information on how to use the header and the handshaking between the client and server works.
   * <p>
   * This method blocks the current thread until the handshake processes completed. Either successfully or with a
   * failure of any kind.
   * <p>
   * If the upgrade to a websocket connection is not possible, the method automatically answers with a reason text to
   * the client why the upgrade failed. Either because the socket version is not supported, or an exception occurred.
   * Supported web socket versions are:
   * <ol>
   *   <li>Wire protocol version 13 (RFC-6455/draft version 17 of the hybi specification)
   * </ol>
   * <p>
   * If the upgrade fails this method returns a task which is completed exceptionally and sends a response to the client
   * indicating the reason for the failure. After that, the connection is still able to send out http requests but will
   * no longer accept / handle these.
   *
   * @return a task completed with the upgraded web socket channel or an exception if the upgrade was not possible.
   */
  @NonNull Task<WebSocketChannel> upgrade();

  /**
   * Get the web socket channel to which this request was upgraded. If the request was not upgraded, or the upgrade was
   * not possible this method returns null.
   *
   * @return the upgraded web socket channel, or null if the connection upgrade was not done or failed.
   */
  @Nullable WebSocketChannel webSocketChanel();

  /**
   * Get the channel to which the request came which is currently processed.
   *
   * @return the channel to which the request came.
   */
  @NonNull HttpChannel channel();

  /**
   * Get the current request which is handled in this context.
   *
   * @return the current request.
   */
  @NonNull HttpRequest request();

  /**
   * Get the current response which will be sent to the client.
   *
   * @return the current response.
   */
  @NonNull HttpResponse response();

  /**
   * Peeks (gets, but does not remove) the last handler in the chain.
   *
   * @return the last handler in the chain, null if there are no handlers.
   */
  @Nullable HttpHandler peekLast();

  /**
   * Sets the current handler as the last handler, cancelling the call of all handlers which are still waiting to be
   * called down the line. This is useful when a handler should set the final result of the request, not allowing any
   * handler to overwrite it.
   *
   * @return true if the cancel state was updated, false otherwise.
   */
  boolean cancelNext();

  /**
   * Sets whether the current handler should be the last handler in the listener call chain, cancelling the call of all
   * handlers which are still waiting to be called down the line. This is useful when a handler should set the final
   * result of the request, not allowing any handler to overwrite it.
   *
   * @param cancelNext if the next handlers in the chain should be skipped.
   * @return the same instance of the context as used to call the method, for chaining.
   */
  @NonNull HttpContext cancelNext(boolean cancelNext);

  /**
   * Get the http component which received the request wrapped by this context.
   *
   * @return the http component which received the request.
   */
  @NonNull HttpComponent<?> component();

  /**
   * Sets whether the connection to client should be closed after the last handler in the chain. This defaults to true.
   * If set to false, the connection will not be closed and the {@code connection} header will automatically be set to
   * {@code keep-alive}. <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive">Mdn docs</a> are
   * providing a more in-depth walk through which other components may be set by a developer to customize the keep alive
   * header.
   *
   * @param value true if the connection should get closed after the last handler, false to keep it open.
   * @return the same instance of the context as used to call the method, for chaining.
   */
  @NonNull HttpContext closeAfter(boolean value);

  /**
   * Sets that the connection to the client should be terminated after the last handler in the chain.
   *
   * @return true if the closing state was updated, false otherwise.
   */
  boolean closeAfter();

  /**
   * Gets a cookie by its name from the current request. Cookies are decoded and encoded in a relaxed (lax) format, this
   * means that duplicate cookies are allowed. If there are multiple cookies with the same name, the first one is
   * returned by this method. If no cookie with the given name is present, this method returns null.
   *
   * @param name the name of the cookie to get.
   * @return the first cookie with the given name sent by the client.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable HttpCookie cookie(@NonNull String name);

  /**
   * Get all cookies which were set in the current request wrapped in this context. Cookies are decoded and encoded in a
   * relaxed (lax) format, this means that duplicate cookies are allowed.
   *
   * @return all cookies set by in the wrapped request.
   */
  @NonNull Collection<HttpCookie> cookies();

  /**
   * Get if a cookie with the given name was sent by the client sending the request to the server. Cookies are decoded
   * and encoded in a relaxed (lax) format, this means that duplicate cookies are allowed. If there are multiple cookies
   * with the same name set in the request this method returns true anyway.
   *
   * @param name the name of the cookie to check for.
   * @return true if a cookie with the given name is present, false otherwise.
   * @throws NullPointerException if the given name is null.
   */
  boolean hasCookie(@NonNull String name);

  /**
   * Sets the given cookies in the response, removing all previously set cookies.
   *
   * @param cookies the new cookies of the response.
   * @return the same instance of the context as used to call the method, for chaining.
   * @throws NullPointerException if the given cookie collection is null.
   */
  @NonNull HttpContext cookies(@NonNull Collection<HttpCookie> cookies);

  /**
   * Adds the given cookie to the response, removing the current one if there is already a cookie with the same name.
   *
   * @param httpCookie the cookie to add.
   * @return the same instance of the context as used to call the method, for chaining.
   * @throws NullPointerException if the given cookie is null.
   */
  @NonNull HttpContext addCookie(@NonNull HttpCookie httpCookie);

  /**
   * Removes the given cookie from the response, if set.
   *
   * @param name the name of the cookie to remove.
   * @return the same instance of the context as used to call the method, for chaining.
   * @throws NullPointerException if the given cookie is null.
   */
  @NonNull HttpContext removeCookie(@NonNull String name);

  /**
   * Removes all cookies previously set in the response.
   *
   * @return the same instance of the context as used to call the method, for chaining.
   */
  @NonNull HttpContext clearCookies();

  /**
   * Get the path of the handler, supplied while registering, which currently handling the request.
   *
   * @return the path of the handler handling the request.
   */
  @NonNull String pathPrefix();

  /**
   * Get all invocation hints which are registered for the given key. Invocation hints are only valid for the current
   * handler call chain, and will be reset after a handler was called. Hints can for example get registered in a http
   * context preprocessor.
   * <p>
   * This method never returns null. If no hints are registered for the given key then an empty collection is returned.
   *
   * @param key the key of the hints to retrieve.
   * @return the registered hints for the given key, or an empty collection if no hints are registered.
   * @throws NullPointerException if the given key is null.
   */
  @NonNull Collection<Object> invocationHints(@NonNull String key);

  /**
   * Adds an invocation hint for the current handler call chain. Invocation hints are only valid for the current handler
   * call chain, and will be reset after a handler was called. Hints can for example get registered in a http context
   * preprocessor.
   * <p>
   * One key can be mapped to multiple hints.
   *
   * @param key   the key of the hint to register.
   * @param value the hint.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given key or hint is null.
   */
  @NonNull HttpContext addInvocationHint(@NonNull String key, @NonNull Object value);

  /**
   * Adds an invocation hint for the current handler call chain. Invocation hints are only valid for the current handler
   * call chain, and will be reset after a handler was called. Hints can for example get registered in a http context
   * preprocessor.
   * <p>
   * One key can be mapped to multiple hints.
   *
   * @param key   the key of the hint to register.
   * @param value the hints to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given key or hint is null.
   */
  @NonNull <T> HttpContext addInvocationHints(@NonNull String key, @NonNull Collection<T> value);
}
