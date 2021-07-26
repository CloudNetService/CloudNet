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

package eu.cloudnetservice.cloudnet.ext.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import eu.cloudnetservice.cloudnet.ext.mongodb.config.MongoDBConnectionConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bson.Document;

public class MongoDBDatabaseProvider extends AbstractDatabaseProvider {

  protected final MongoDBConnectionConfig config;
  protected final ExecutorService executorService;
  protected final boolean autoShutdownExecutorService;
  protected final Map<String, Database> cachedDatabaseInstances;

  protected MongoClient mongoClient;
  protected MongoDatabase mongoDatabase;

  public MongoDBDatabaseProvider(MongoDBConnectionConfig config) {
    this(config, null);
  }

  public MongoDBDatabaseProvider(MongoDBConnectionConfig config, ExecutorService executorService) {
    this.config = config;
    this.cachedDatabaseInstances = new ConcurrentHashMap<>();
    this.autoShutdownExecutorService = executorService == null;
    this.executorService = executorService == null ? Executors.newCachedThreadPool() : executorService;
  }

  @Override
  public boolean init() throws Exception {
    this.mongoClient = MongoClients.create(this.config.buildConnectionUri());
    this.mongoDatabase = this.mongoClient.getDatabase(this.config.getDatabase());

    return true;
  }

  @Override
  public Database getDatabase(String name) {
    return this.cachedDatabaseInstances.computeIfAbsent(name, $ -> {
      MongoCollection<Document> collection = this.mongoDatabase.getCollection(name);
      return new MongoDBDatabase(name, collection, this.executorService, this);
    });
  }

  @Override
  public boolean containsDatabase(String name) {
    return this.getDatabaseNames().contains(name);
  }

  @Override
  public boolean deleteDatabase(String name) {
    this.cachedDatabaseInstances.remove(name);
    this.mongoDatabase.getCollection(name).drop();

    return true;
  }

  @Override
  public Collection<String> getDatabaseNames() {
    return this.mongoDatabase.listCollectionNames().into(new ArrayList<>());
  }

  @Override
  public boolean needsClusterSync() {
    return false;
  }

  @Override
  public void close() {
    this.mongoClient.close();
    this.cachedDatabaseInstances.clear();
  }

  @Override
  public String getName() {
    return "mongodb";
  }
}
