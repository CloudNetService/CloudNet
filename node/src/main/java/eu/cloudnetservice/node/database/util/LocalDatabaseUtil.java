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

package eu.cloudnetservice.node.database.util;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import lombok.NonNull;

public final class LocalDatabaseUtil {

  private static final Logger LOGGER = LogManager.logger(LocalDatabaseUtil.class);

  private LocalDatabaseUtil() {
    throw new UnsupportedOperationException();
  }

  public static void bigWarningThatEveryoneCanSee(@NonNull String warning) {
    LOGGER.warning("╔══════════════════════════════════════════════════════════════════╗");
    LOGGER.warning("║                               WARNING                             ");
    LOGGER.warning("║   " + warning);
    LOGGER.warning("║                                                                   ");
    LOGGER.warning("║                                                                   ");
    LOGGER.warning("║        https://cloudnetservice.eu/docs/3.4/setup/cluster          ");
    LOGGER.warning("╚══════════════════════════════════════════════════════════════════╝");
  }
}
