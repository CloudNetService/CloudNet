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

package eu.cloudnetservice.node.command.sub;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import eu.cloudnetservice.node.command.annotation.Description;
import jakarta.inject.Singleton;
import org.slf4j.LoggerFactory;

@Singleton
@Description("command-debug-description")
@CommandPermission("cloudnet.command.debug")
public final class DebugCommand {

  @CommandMethod("debug")
  public void debug() {
    var rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    if (rootLogger instanceof Logger logbackLogger) {
      if (rootLogger.isDebugEnabled()) {
        // TODO: where do we get the level from?
        logbackLogger.setLevel(ch.qos.logback.classic.Level.INFO);
      } else {
        logbackLogger.setLevel(Level.TRACE);
      }
    }
  }
}
