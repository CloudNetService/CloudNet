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

package eu.cloudnetservice.common.log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * The fallback logger factory which creates the loggers based on java.util.logging loggers which get wrapped into
 * custom loggers. This factory caches the logger instances which means that demanding a logger with the same name twice
 * results in the same logger instance each time.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class FallbackLoggerFactory implements LoggerFactory {

  private final Map<String, Logger> createdLoggers = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Logger logger(@NonNull String name) {
    var registered = LogManager.getLogManager().getLogger(name);
    // check if this logger is already a wrapped logger
    if (registered instanceof Logger) {
      return (Logger) registered;
    }
    // get the logger from the cache or create a new one and put it into
    return this.createdLoggers.computeIfAbsent(name, $ -> {
      if (registered == null) {
        // no logger is yet registered to the java log manager - do so
        var julComputedLoggerInstance = Logger.getLogger(name);
        return new FallbackPassthroughLogger(julComputedLoggerInstance);
      } else {
        // the logger is already there but not in a wrapped form - just wrap it
        return new FallbackPassthroughLogger(registered);
      }
    });
  }
}
