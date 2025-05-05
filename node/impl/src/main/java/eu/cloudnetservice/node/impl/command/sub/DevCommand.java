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

package eu.cloudnetservice.node.impl.command.sub;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.slf4j.LoggerFactory;

@Singleton
@Description("command-dev-description")
@Permission("cloudnet.command.dev")
public final class DevCommand {

  private static final List<String> LOG_LEVEL = List.of("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE");

  @Parser(suggestions = "logLevel")
  public @NonNull Level logLevelParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var levelName = input.readString();
    var level = Level.toLevel(levelName, null);
    if (level == null) {
      throw new ArgumentNotAvailableException(String.format("The provided log level %s does not exist", levelName));
    }

    return level;
  }

  @Suggestions("logLevel")
  public @NonNull List<String> suggestLogLevel() {
    return LOG_LEVEL;
  }

  @Command("dev set logLevel <level>")
  public void setLogLevel(@NonNull @Service I18n i18n, @NonNull CommandSource source, @NonNull @Argument("level") Level level) {
    var rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    if (rootLogger instanceof Logger logbackLogger) {
      source.sendMessage(String.format("The log level was set to %s", level));
      logbackLogger.setLevel(level);
    }
  }
}
