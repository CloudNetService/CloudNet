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

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;

@Singleton
@CommandAlias({"ask", "?"})
@Permission("cloudnet.command.help")
@Description("command-help-description")
public final class HelpCommand {

  private static final RowedFormatter<CommandInfo> HELP_LIST_FORMATTER = RowedFormatter.<CommandInfo>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name(s)", "Description", "Permission").build())
    .column(info -> info.joinNameToAliases(", "))
    .column(CommandInfo::description)
    .column(CommandInfo::permission)
    .build();

  private final CommandProvider commandProvider;

  @Inject
  public HelpCommand(@NonNull CommandProvider commandProvider) {
    this.commandProvider = commandProvider;
  }

  @Parser(suggestions = "commands")
  public @NonNull CommandInfo defaultCommandInfoParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var command = input.readString();
    var commandInfo = this.commandProvider.command(command);
    if (commandInfo == null) {
      throw new ArgumentNotAvailableException(i18n.translate("no-such-command"));
    }

    return commandInfo;
  }

  @Suggestions("commands")
  public @NonNull Stream<String> suggestCommands() {
    return this.commandProvider.commands().stream().map(Named::name);
  }

  @Command("help|ask|?")
  public void displayHelp(@NonNull CommandSource source) {
    source.sendMessage(HELP_LIST_FORMATTER.format(this.commandProvider.commands()));
  }

  @Command("help|ask|? <command>")
  public void displaySpecificHelp(@NonNull CommandSource source, @NonNull @Argument("command") CommandInfo command) {
    source.sendMessage("Names: " + command.joinNameToAliases(", "));
    source.sendMessage("Description: " + command.description());
    source.sendMessage("Usage: ");
    for (var usage : command.usage()) {
      source.sendMessage(" - " + usage);
    }
  }

  @Command("help|ask|? docs <command>")
  public void displayCommandDocs(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("command") CommandInfo commandInfo
  ) {
    if (commandInfo.docsUrl() == null) {
      source.sendMessage(i18n.translate("command-help-docs-no-url", commandInfo.name()));
    } else {
      source.sendMessage(i18n.translate("command-help-docs", commandInfo.name(), commandInfo.docsUrl()));
    }
  }
}
