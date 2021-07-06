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

package de.dytanic.cloudnet.common.logging;

/**
 * An interface for a provider, which provides all log handlers for a log handlers
 */
public interface ILogHandlerProvider<T extends ILogHandlerProvider<?>> {

  /**
   * Adds a new ILogHandler instance, into the collection by the LogHandlerProvider implementation
   *
   * @param logHandler the ILogHandler implementation, which should append
   * @return the current implementation of the ILogHandlerProvider
   */
  T addLogHandler(ILogHandler logHandler);

  /**
   * Adds an array of ILogHandler instances, into the collection by the LogHandlerProvider implementation
   *
   * @param logHandlers the ILogHandler's implementation, which should append
   * @return the current implementation of the ILogHandlerProvider
   */
  T addLogHandlers(ILogHandler... logHandlers);

  /**
   * Adds an Iterable of ILogHandler instances, into the collection by the LogHandlerProvider implementation
   *
   * @param logHandlers the ILogHandler's implementation, which should append
   * @return the current implementation of the ILogHandlerProvider
   */
  T addLogHandlers(Iterable<ILogHandler> logHandlers);

  /**
   * Removes when contains the ILogHandler reference into the internal registry
   *
   * @param logHandler the logHandler, which should be removed
   * @return the current implementation of the ILogHandlerProvider
   */
  T removeLogHandler(ILogHandler logHandler);

  /**
   * Removes when contains the ILogHandler's reference into the internal registry
   *
   * @param logHandlers the ILogHandler's, which should be removed
   * @return the current implementation of the ILogHandlerProvider
   */
  T removeLogHandlers(ILogHandler... logHandlers);

  /**
   * Removes when contains the ILogHandler's reference into the internal registry
   *
   * @param logHandlers the ILogHandler's, which should be removed
   * @return the current implementation of the ILogHandlerProvider
   */
  T removeLogHandlers(Iterable<ILogHandler> logHandlers);

  /**
   * Returns all registered ILogHandler instances as an Iterable
   */
  Iterable<ILogHandler> getLogHandlers();

  /**
   * Check that the ILogHandler exists on this provider
   *
   * @param logHandler the ILogHandler, that should test
   * @return true if the ILogHandler instance contain on this LogHandlerProvider object
   */
  boolean hasLogHandler(ILogHandler logHandler);

  /**
   * Check that the ILogHandler's exists on this provider
   *
   * @param logHandlers the ILogHandler's, that should test
   * @return true if the ILogHandler's instances contain on this LogHandlerProvider object
   */
  boolean hasLogHandlers(ILogHandler... logHandlers);
}
