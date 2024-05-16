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

import lombok.NonNull;

/**
 * A factory for logger objects.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface LoggerFactory {

  /**
   * The name of the root logger used by the system. Editing the associated logger will result in all loggers to
   * change.
   */
  String ROOT_LOGGER_NAME = "";

  /**
   * Gets or creates a logger with the given name. Overridden methods may but must no cache created logger objects,
   * meaning that this method can also create a new logger each time this method is called.
   *
   * @param name the name of the logger.
   * @return the cached or created logger object having the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull Logger logger(@NonNull String name);
}
