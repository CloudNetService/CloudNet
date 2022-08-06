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

package eu.cloudnetservice.modules.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import eu.cloudnetservice.modules.mongodb.config.MongoDBConnectionConfig;
import eu.cloudnetservice.node.database.AbstractDatabaseProvider;
import eu.cloudnetservice.node.database.LocalDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class MongoDBDatabaseProvider extends AbstractDatabaseProvider {

  protected final MongoDBConnectionConfig config;
  protected final ExecutorService executorService;
  protected final boolean autoShutdownExecutorService;

  protected MongoClient mongoClient;
  protected MongoDatabase mongoDatabase;

  public MongoDBDatabaseProvider(@NonNull MongoDBConnectionConfig config) {
    this(config, null);
  }

  public MongoDBDatabaseProvider(@NonNull MongoDBConnectionConfig config, @Nullable ExecutorService executorService) {
    this.config = config;
    this.autoShutdownExecutorService = executorService == null;
    this.executorService = executorService == null ? Executors.newCachedThreadPool() : executorService;
  }

  @Override
  public boolean init() {
    this.mongoClient = MongoClients.create(this.config.buildConnectionUri());
    this.mongoDatabase = this.mongoClient.getDatabase(this.config.database());

    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, $ -> {
      var collection = this.mongoDatabase.getCollection(name);
      return new MongoDBDatabase(name, collection, this.executorService, this);
    });
  }

  @Override
  public boolean containsDatabase(@NonNull String name) {
    return this.databaseNames().contains(name);
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    this.databaseCache.invalidate(name);
    this.mongoDatabase.getCollection(name).drop();

    return true;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    return this.mongoDatabase.listCollectionNames().into(new ArrayList<>());
  }

  @Override
  public void close() throws Exception {
    super.close();
    this.mongoClient.close();

    if (this.autoShutdownExecutorService) {
      this.executorService.shutdownNow();
    }
  }

  @Override
  public @NonNull String name() {
    return "mongodb";
  }
}
