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
    if (context.getInputQueue().isEmpty()) {
      return this.provider.getCommands().stream().map(INameable::getName).collect(Collectors.toList());
    }

    String input = context.getInputQueue().peek();
    List<String> suggestions = new LinkedList<>();

    for (String suggestion : strings) {
      if (suggestion.startsWith(input)
        && (context.getCommandContext().getRawInput().size() > 1 || this.provider.getCommand(suggestion) != null)) {
        suggestions.add(suggestion);
      }
    }
    return suggestions;
  }
}
