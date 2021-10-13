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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.log.LoggingUtils;
import java.util.logging.Level;

@CommandPermission("cloudnet.command.debug")
public class CommandDebug {

  @CommandDescription("Toggle the global debug mode")
  @CommandMethod("debug")
  public void debug() {
    Logger rootLogger = LogManager.getRootLogger();
    if (rootLogger.isLoggable(Level.FINEST)) {
      rootLogger.setLevel(LoggingUtils.getDefaultLogLevel());
    } else {
      rootLogger.setLevel(Level.FINEST);
    }
  }

}
