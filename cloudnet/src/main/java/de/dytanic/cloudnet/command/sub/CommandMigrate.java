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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.source.ConsoleCommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableConsumer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.LocalDatabase;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@CommandPermission("cloudnet.command.migrate")
public class CommandMigrate {

  private static final int DEFAULT_CHUNK_SIZE = 100;
  private static final Logger LOGGER = LogManager.getLogger(CommandMigrate.class);

  @Parser(suggestions = "databaseProvider")
  public AbstractDatabaseProvider defaultDatabaseProviderParser(CommandContext<CommandSource> $, Queue<String> input) {
    AbstractDatabaseProvider abstractDatabaseProvider = CloudNet.getInstance().getServicesRegistry()
      .getService(AbstractDatabaseProvider.class, input.remove());

    if (abstractDatabaseProvider == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-migrate-unknown-database-provider"));
    }
    return abstractDatabaseProvider;
  }

  @Suggestions("databaseProvider")
  public List<String> suggestDatabaseProvider(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getServicesRegistry().getServices(AbstractDatabaseProvider.class)
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  @CommandMethod(value = "migrate database <database-from> <database-to>", requiredSender = ConsoleCommandSource.class)
  public void migrateDatabase(
    CommandSource source,
    @Argument("database-from") AbstractDatabaseProvider sourceDatabaseProvider,
    @Argument("database-to") AbstractDatabaseProvider targetDatabaseProvider,
    @Flag("chunk-size") Integer chunkSize
  ) {
    if (sourceDatabaseProvider.equals(targetDatabaseProvider)) {
      source.sendMessage(LanguageManager.getMessage("command-migrate-source-equals-target"));
      return;
    }

    if (chunkSize == null || chunkSize <= 0) {
      chunkSize = DEFAULT_CHUNK_SIZE;
    }

    if (!this.executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::init)
      || this.executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::init)) {
      return;
    }

    try {
      for (String databaseName : sourceDatabaseProvider.getDatabaseNames()) {
        source.sendMessage(
          LanguageManager.getMessage("command-migrate-current-database").replace("%db%", databaseName));

        LocalDatabase sourceDatabase = sourceDatabaseProvider.getDatabase(databaseName);
        LocalDatabase targetDatabase = targetDatabaseProvider.getDatabase(databaseName);

        sourceDatabase.iterate(targetDatabase::insert, chunkSize);
      }
    } catch (Exception exception) {
      LOGGER.severe(
        LanguageManager.getMessage("command-migrate-database-connection-failed"), exception);
      return;
    }

    executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::close);
    executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::close);

    source.sendMessage(LanguageManager.getMessage("command-migrate-success")
      .replace("%source%", sourceDatabaseProvider.getName())
      .replace("%target%", targetDatabaseProvider.getName()));
  }

  private boolean executeIfNotCurrentProvider(@NotNull AbstractDatabaseProvider sourceProvider,
    @NotNull ThrowableConsumer<AbstractDatabaseProvider, ?> handler) {
    if (!CloudNet.getInstance().getDatabaseProvider().equals(sourceProvider)) {
      try {
        handler.accept(sourceProvider);
      } catch (Throwable throwable) {
        LOGGER.severe(
          LanguageManager.getMessage("command-migrate-database-connection-failed"), throwable);
        return false;
      }
    }
    return true;
  }

}
