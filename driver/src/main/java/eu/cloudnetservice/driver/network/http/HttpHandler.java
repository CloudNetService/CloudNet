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

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A handler for a http request. Each request, matching the given attributes supplied when registering, will be called
 * directly into this handler. A request is only processed by one handler at a time. Handlers with a high priority will
 * get called before handlers with a low priority.
 *
 * @since 4.0
 */
public abstract class HttpHandler {

  public static final int PRIORITY_HIGH = 64;
  public static final int PRIORITY_NORMAL = 32;
  public static final int PRIORITY_LOW = 16;
  public static final int PRIORITY_LOWEST = 0;

  private final Deque<HttpContextPreprocessor> preprocessors = new LinkedList<>();

  /**
   * Adds a preprocessor which is applied to the context before calling this handler. The pre-processors are called in
   * the order they were added, using this method the handler is put at the head of the listeners, meaning that all
   * other previously added handlers will be called after.
   *
   * @param preprocessor the context preprocessor to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given preprocessor is null.
   */
  public @NonNull HttpHandler addPreProcessorHead(@NonNull HttpContextPreprocessor preprocessor) {
    this.preprocessors.addFirst(preprocessor);
    return this;
  }

  /**
   * Adds a preprocessor which is applied to the context before calling this handler. The pre-processors are called in
   * the order they were added, using this method the handler is put at the tail of the listeners, meaning that all
   * other previously added handlers will be called first.
   *
   * @param preprocessor the context preprocessor to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given preprocessor is null.
   */
  public @NonNull HttpHandler addPreProcessorTail(@NonNull HttpContextPreprocessor preprocessor) {
    this.preprocessors.addLast(preprocessor);
    return this;
  }

  /**
   * Gets all preprocessors which were added to this http handler.
   *
   * @return all registered preprocessors.
   */
  @UnmodifiableView
  public @NonNull Collection<HttpContextPreprocessor> preprocessors() {
    return Collections.unmodifiableCollection(this.preprocessors);
  }

  /**
   * Handles a http request whose path (and other supplied attributes) while registering is matching the requested path
   * of the client. A request is only processed by one handler at a time, giving the handler full control about changing
   * the context. Changes to the context will be reflected into other handlers and vise-versa.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws Throwable            if any exception occurs during the request handling.
   * @throws NullPointerException if the given path or context is null.
   */
  public abstract void handle(@NonNull String path, @NonNull HttpContext context) throws Throwable;
}
