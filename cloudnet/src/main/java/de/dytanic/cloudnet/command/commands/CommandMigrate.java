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

package de.dytanic.cloudnet.command.commands;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableConsumer;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CommandMigrate extends SubCommandHandler {

  public CommandMigrate() {
    super(
      SubCommandBuilder.create()
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            AbstractDatabaseProvider sourceDatabaseProvider = getDatabaseProvider((String) args.argument(1));
            AbstractDatabaseProvider targetDatabaseProvider = getDatabaseProvider((String) args.argument(2));

            if (sourceDatabaseProvider.equals(targetDatabaseProvider)) {
              sender.sendMessage(LanguageManager.getMessage("command-migrate-source-equals-target"));
              return;
            }

            executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::init);
            executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::init);

            for (String databaseName : sourceDatabaseProvider.getDatabaseNames()) {
              if (!properties.containsKey("no-clear") || !properties.getBoolean("no-clear")) {
                targetDatabaseProvider.deleteDatabase(databaseName);
              }

              sender.sendMessage(
                LanguageManager.getMessage("command-migrate-current-database").replace("%db%", databaseName));

              Database sourceDatabase = sourceDatabaseProvider.getDatabase(databaseName);
              Database targetDatabase = targetDatabaseProvider.getDatabase(databaseName);

              for (Entry<String, JsonDocument> databaseEntry : sourceDatabase.entries().entrySet()) {
                targetDatabase.insert(databaseEntry.getKey(), databaseEntry.getValue());
              }
            }

            executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::close);
            executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::close);

            sender.sendMessage(LanguageManager.getMessage("command-migrate-success")
              .replace("%source%", sourceDatabaseProvider.getName())
              .replace("%target%", targetDatabaseProvider.getName()));
          },
          command -> command.enableProperties().appendUsage("| --no-clear"),
          anyStringIgnoreCase("database", "db"),
          dynamicString("database-from", LanguageManager.getMessage("command-migrate-unknown-database-provider"),
            providerRegisteredPredicate(),
            possibleAnswerSupplier()),
          dynamicString("database-to", LanguageManager.getMessage("command-migrate-unknown-database-provider"),
            providerRegisteredPredicate(),
            possibleAnswerSupplier())
        )
        .getSubCommands(),
      "migrate"
    );
    super.prefix = "cloudnet";
    super.permission = "cloudnet.command." + super.names[0];
    super.description = LanguageManager.getMessage("command-description-migrate");
  }

  private static Predicate<String> providerRegisteredPredicate() {
    return input -> getDatabaseProvider(input) != null;
  }

  private static Supplier<Collection<String>> possibleAnswerSupplier() {
    return () -> CloudNet.getInstance().getServicesRegistry().getServices(AbstractDatabaseProvider.class)
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  private static AbstractDatabaseProvider getDatabaseProvider(@NotNull String name) {
    return CloudNet.getInstance().getServicesRegistry().getService(AbstractDatabaseProvider.class, name);
  }

  private static void executeIfNotCurrentProvider(@NotNull AbstractDatabaseProvider provider,
    @NotNull ThrowableConsumer<AbstractDatabaseProvider, ?> handler) {
    if (!CloudNet.getInstance().getDatabaseProvider().equals(provider)) {
      try {
        handler.accept(provider);
      } catch (Throwable throwable) {
        throwable.printStackTrace();
      }
    }
  }
}
