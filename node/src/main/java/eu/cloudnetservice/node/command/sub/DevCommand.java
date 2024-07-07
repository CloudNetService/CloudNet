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
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;
import org.slf4j.LoggerFactory;

@Singleton
@Description("command-dev-description")
@CommandPermission("cloudnet.command.dev")
public final class DevCommand {

  private static final List<String> LOG_LEVEL = List.of("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "OFF");

  @Parser(suggestions = "logLevel")
  public @NonNull Level logLevelParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var levelName = input.remove();
    var level = Level.toLevel(levelName, null);
    if (level == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-dev-log-level-not-found"));
    }

    return level;
  }

  @Suggestions("logLevel")
  public @NonNull List<String> suggestLogLevel(@NonNull CommandContext<?> $, @NonNull String input) {
    return LOG_LEVEL;
  }

  @CommandMethod("dev set logLevel <level>")
  public void setLogLevel(@NonNull CommandSource source, @NonNull @Argument("level") Level level) {
    var rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    if (rootLogger instanceof Logger logbackLogger) {
      source.sendMessage(I18n.trans("command-dev-log-level-set", level.toString()));
      logbackLogger.setLevel(level);
    }
  }
}
