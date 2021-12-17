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

package de.dytanic.cloudnet.command.defaults;

import cloud.commandframework.execution.CommandSuggestionProcessor;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.StringUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;

final class DefaultSuggestionProcessor implements CommandSuggestionProcessor<CommandSource> {

  private final CommandProvider provider;

  public DefaultSuggestionProcessor(CommandProvider provider) {
    this.provider = provider;
  }

  @Override
  public @NonNull List<String> apply(
    @NonNull CommandPreprocessingContext<CommandSource> context,
    @NonNull List<String> strings
  ) {
    // check if the user tries to complete all command roots
    if (!context.getCommandContext().getRawInputJoined().contains(" ")) {
      return this.provider.getCommands().stream().map(INameable::name).collect(Collectors.toList());
    }
    // is the queue is empty just use a blank string.
    String input;
    if (context.getInputQueue().isEmpty()) {
      input = "";
    } else {
      input = context.getInputQueue().peek();
    }

    List<String> suggestions = new LinkedList<>();
    for (var suggestion : strings) {
      // check if clouds suggestion matches the input
      if (StringUtil.startsWithIgnoreCase(suggestion, input)) {
        // validate that the command is registered
        var rawInput = context.getCommandContext().getRawInput();
        if (rawInput.size() > 1) {
          // there are already arguments - validate that the command root is registered before suggesting further arguments
          if (this.provider.getCommand(rawInput.get(0)) != null) {
            suggestions.add(suggestion);
          }
        } else {
          // if there are no arguments yet, just validate that the suggestion is registered as a command
          if (this.provider.getCommand(suggestion) != null) {
            suggestions.add(suggestion);
          }
        }
      }
    }
    return suggestions;
  }
}
