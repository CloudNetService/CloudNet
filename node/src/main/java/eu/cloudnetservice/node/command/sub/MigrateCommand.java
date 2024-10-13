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

import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import io.vavr.CheckedConsumer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Permission("cloudnet.command.migrate")
@Description("command-migrate-description")
public final class MigrateCommand {

  private static final int DEFAULT_CHUNK_SIZE = 100;
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateCommand.class);

  private final ServiceRegistry serviceRegistry;
  private final DatabaseProvider databaseProvider;

  @Inject
  public MigrateCommand(@NonNull ServiceRegistry serviceRegistry, @NonNull DatabaseProvider databaseProvider) {
    this.serviceRegistry = serviceRegistry;
    this.databaseProvider = databaseProvider;
  }

  @Parser(suggestions = "databaseProvider")
  public @NonNull NodeDatabaseProvider defaultDatabaseProviderParser(@NonNull CommandInput input) {
    var abstractDatabaseProvider = this.serviceRegistry.provider(NodeDatabaseProvider.class, input.readString());

    if (abstractDatabaseProvider == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-migrate-unknown-database-provider"));
    }
    return abstractDatabaseProvider;
  }

  @Suggestions("databaseProvider")
  public @NonNull Stream<String> suggestDatabaseProvider() {
    return this.serviceRegistry.providers(NodeDatabaseProvider.class)
      .stream()
      .map(Named::name);
  }

  @Command(value = "migrate database|db <database-from> <database-to>", requiredSender = ConsoleCommandSource.class)
  public void migrateDatabase(
    @NonNull CommandSource source,
    @NonNull @Argument("database-from") NodeDatabaseProvider sourceDatabaseProvider,
    @NonNull @Argument("database-to") NodeDatabaseProvider targetDatabaseProvider,
    @Flag("chunk-size") Integer chunkSize
  ) {
    if (sourceDatabaseProvider.equals(targetDatabaseProvider)) {
      source.sendMessage(I18n.trans("command-migrate-source-equals-target"));
      return;
    }

    if (chunkSize == null || chunkSize <= 0) {
      chunkSize = DEFAULT_CHUNK_SIZE;
    }

    if (!this.executeIfNotCurrentProvider(sourceDatabaseProvider, NodeDatabaseProvider::init)
      || !this.executeIfNotCurrentProvider(targetDatabaseProvider, NodeDatabaseProvider::init)) {
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
      LOGGER.error(I18n.trans("command-migrate-database-connection-failed"), exception);
      return;
    }

    this.executeIfNotCurrentProvider(sourceDatabaseProvider, NodeDatabaseProvider::close);
    this.executeIfNotCurrentProvider(targetDatabaseProvider, NodeDatabaseProvider::close);

    source.sendMessage(I18n.trans("command-migrate-success",
      sourceDatabaseProvider.name(),
      targetDatabaseProvider.name()));
  }

  private boolean executeIfNotCurrentProvider(
    @NonNull NodeDatabaseProvider sourceProvider,
    @NonNull CheckedConsumer<NodeDatabaseProvider> handler
  ) {
    if (!this.databaseProvider.equals(sourceProvider)) {
      try {
        handler.accept(sourceProvider);
      } catch (Throwable throwable) {
        LOGGER.error(I18n.trans("command-migrate-database-connection-failed"), throwable);
        return false;
      }
    }

    return true;
  }
}
