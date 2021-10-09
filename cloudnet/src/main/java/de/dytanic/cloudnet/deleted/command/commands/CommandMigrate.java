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

package de.dytanic.cloudnet.deleted.command.commands;

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableConsumer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.deleted.command.sub.SubCommandArgumentTypes;
import de.dytanic.cloudnet.deleted.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.deleted.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.driver.database.Database;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CommandMigrate extends SubCommandHandler {

  private static final int DEFAULT_CHUNK_SIZE = 100;

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

            if (!executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::init) ||
              !executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::init)) {
              return;
            }

            Integer chunkSize = Ints.tryParse(
              properties.getOrDefault("chunk-size", Integer.toString(DEFAULT_CHUNK_SIZE)));
            if (chunkSize == null) {
              sender.sendMessage(LanguageManager.getMessage("command-migrate-chunk-size-not-a-number"));
              return;
            }

            try {
              for (String databaseName : sourceDatabaseProvider.getDatabaseNames()) {
                sender.sendMessage(
                  LanguageManager.getMessage("command-migrate-current-database").replace("%db%", databaseName));

                Database sourceDatabase = sourceDatabaseProvider.getDatabase(databaseName);
                Database targetDatabase = targetDatabaseProvider.getDatabase(databaseName);

                sourceDatabase.iterate(targetDatabase::insert, chunkSize);
              }
            } catch (Exception exception) {
              CloudNet.getInstance().getLogger().error(
                LanguageManager.getMessage("command-migrate-database-connection-failed"), exception);
              return;
            }

            executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::close);
            executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::close);

            sender.sendMessage(LanguageManager.getMessage("command-migrate-success")
              .replace("%source%", sourceDatabaseProvider.getName())
              .replace("%target%", targetDatabaseProvider.getName()));
          },
          command -> command.onlyConsole().enableProperties().appendUsage("| --chunk-size=100"),
          SubCommandArgumentTypes.anyStringIgnoreCase("database", "db"),
          SubCommandArgumentTypes.dynamicString("database-from",
            LanguageManager.getMessage("command-migrate-unknown-database-provider"),
            providerRegisteredPredicate(),
            possibleDatabaseProvidersSupplier()),
          SubCommandArgumentTypes.dynamicString("database-to",
            LanguageManager.getMessage("command-migrate-unknown-database-provider"),
            providerRegisteredPredicate(),
            possibleDatabaseProvidersSupplier())
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

  private static Supplier<Collection<String>> possibleDatabaseProvidersSupplier() {
    return () -> CloudNet.getInstance().getServicesRegistry().getServices(AbstractDatabaseProvider.class)
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  private static AbstractDatabaseProvider getDatabaseProvider(@NotNull String name) {
    return CloudNet.getInstance().getServicesRegistry().getService(AbstractDatabaseProvider.class, name);
  }

  private static boolean executeIfNotCurrentProvider(@NotNull AbstractDatabaseProvider sourceProvider,
    @NotNull ThrowableConsumer<AbstractDatabaseProvider, ?> handler) {
    if (!CloudNet.getInstance().getDatabaseProvider().equals(sourceProvider)) {
      try {
        handler.accept(sourceProvider);
      } catch (Throwable throwable) {
        CloudNet.getInstance().getLogger().error(
          LanguageManager.getMessage("command-migrate-database-connection-failed"), throwable);
        return false;
      }
    }
    return true;
  }
}
