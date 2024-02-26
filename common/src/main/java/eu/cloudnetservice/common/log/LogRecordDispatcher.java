/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.common.log;

import java.util.logging.LogRecord;
import lombok.NonNull;

/**
 * Represents a dispatcher for log records. This dispatcher is meant to pre-process log records if needed and then post
 * them to all registered dispatchers of a logger, for example from an asynchronous context.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface LogRecordDispatcher {

  /**
   * Called when a log record needs to be dispatched for a logger.
   *
   * @param logger the logger from which the log event came.
   * @param record the record which needs to be dispatched.
   * @throws NullPointerException if either the given logger or record is null.
   */
  void dispatchRecord(@NonNull Logger logger, @NonNull LogRecord record);
}
