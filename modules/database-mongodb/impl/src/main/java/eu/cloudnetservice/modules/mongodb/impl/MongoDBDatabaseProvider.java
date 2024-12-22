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

package eu.cloudnetservice.modules.mongodb.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import eu.cloudnetservice.modules.mongodb.config.MongoDBConnectionConfig;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.AbstractNodeDatabaseProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.NonNull;

public class MongoDBDatabaseProvider extends AbstractNodeDatabaseProvider {

  protected final MongoDBConnectionConfig config;

  protected MongoClient mongoClient;
  protected MongoDatabase mongoDatabase;

  public MongoDBDatabaseProvider(@NonNull MongoDBConnectionConfig config) {
    super(DEFAULT_REMOVAL_LISTENER);
    this.config = config;
  }

  @Override
  public boolean init() {
    // mongo db prints some information about the created client. We don't want that information
    Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);

    this.mongoClient = MongoClients.create(this.config.buildConnectionUri());
    this.mongoDatabase = this.mongoClient.getDatabase(this.config.database());

    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, $ -> {
      var collection = this.mongoDatabase.getCollection(name);
      return new MongoDBDatabase(name, collection, this);
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
  }

  @Override
  public @NonNull String name() {
    return this.config.databaseServiceName();
  }
}
