/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.function.ThrowableConsumer;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.database.AbstractDatabaseProvider;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;

@CommandPermission("cloudnet.command.migrate")
@Description("command-migrate-description")
public final class MigrateCommand {

  private static final int DEFAULT_CHUNK_SIZE = 100;
  private static final Logger LOGGER = LogManager.logger(MigrateCommand.class);

  @Parser(suggestions = "databaseProvider")
  public @NonNull AbstractDatabaseProvider defaultDatabaseProviderParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var abstractDatabaseProvider = Node.instance().serviceRegistry()
      .provider(AbstractDatabaseProvider.class, input.remove());

    if (abstractDatabaseProvider == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-migrate-unknown-database-provider"));
    }
    return abstractDatabaseProvider;
  }

  @Suggestions("databaseProvider")
  public @NonNull List<String> suggestDatabaseProvider(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().serviceRegistry().providers(AbstractDatabaseProvider.class)
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @CommandMethod(value = "migrate database|db <database-from> <database-to>", requiredSender = ConsoleCommandSource.class)
  public void migrateDatabase(
    @NonNull CommandSource source,
    @NonNull @Argument("database-from") AbstractDatabaseProvider sourceDatabaseProvider,
    @NonNull @Argument("database-to") AbstractDatabaseProvider targetDatabaseProvider,
    @Flag("chunk-size") Integer chunkSize
  ) {
    if (sourceDatabaseProvider.equals(targetDatabaseProvider)) {
      source.sendMessage(I18n.trans("command-migrate-source-equals-target"));
      return;
    }

    if (chunkSize == null || chunkSize <= 0) {
      chunkSize = DEFAULT_CHUNK_SIZE;
    }

    if (!this.executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::init)
      || !this.executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::init)) {
      return;
    }

    try {
      for (var databaseName : sourceDatabaseProvider.databaseNames()) {
        source.sendMessage(
          I18n.trans("command-migrate-current-database", databaseName));

        var sourceDatabase = sourceDatabaseProvider.database(databaseName);
        var targetDatabase = targetDatabaseProvider.database(databaseName);

        sourceDatabase.iterate(targetDatabase::insert, chunkSize);
      }
    } catch (Exception exception) {
      LOGGER.severe(I18n.trans("command-migrate-database-connection-failed"), exception);
      return;
    }

    this.executeIfNotCurrentProvider(sourceDatabaseProvider, AbstractDatabaseProvider::close);
    this.executeIfNotCurrentProvider(targetDatabaseProvider, AbstractDatabaseProvider::close);

    source.sendMessage(I18n.trans("command-migrate-success",
      sourceDatabaseProvider.name(),
      targetDatabaseProvider.name()));
  }

  private boolean executeIfNotCurrentProvider(
    @NonNull AbstractDatabaseProvider sourceProvider,
    @NonNull ThrowableConsumer<AbstractDatabaseProvider, ?> handler
  ) {
    if (!Node.instance().databaseProvider().equals(sourceProvider)) {
      try {
        handler.accept(sourceProvider);
      } catch (Throwable throwable) {
        LOGGER.severe(I18n.trans("command-migrate-database-connection-failed"), throwable);
        return false;
      }
    }
    return true;
  }
}
