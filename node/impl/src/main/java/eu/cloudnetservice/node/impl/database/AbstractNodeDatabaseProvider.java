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

package eu.cloudnetservice.node.impl.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNodeDatabaseProvider implements NodeDatabaseProvider {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractNodeDatabaseProvider.class);
  protected static final RemovalListener<String, LocalDatabase> DEFAULT_REMOVAL_LISTENER = (key, value, cause) -> {
    // close the database instance that was removed, unless the database instance was garbage collected
    if (value != null) {
      try {
        value.close();
      } catch (Exception exception) {
        LOGGER.error("Exception closing removed database instance {}", value.name(), exception);
      }
    }
  };

  protected final Cache<String, LocalDatabase> databaseCache;

  protected AbstractNodeDatabaseProvider(@NonNull RemovalListener<String, LocalDatabase> removalListener) {
    this.databaseCache = Caffeine.newBuilder()
      .scheduler(Scheduler.systemScheduler())
      .expireAfterAccess(Duration.ofMinutes(5))
      .removalListener(removalListener)
      .build();
  }

  @Override
  public void close() throws Exception {
    this.databaseCache.invalidateAll();
  }

  @Override
  public @NonNull CompletableFuture<Collection<String>> databaseNamesAsync() {
    return TaskUtil.supplyAsync(this::databaseNames);
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deleteDatabaseAsync(@NonNull String name) {
    return TaskUtil.supplyAsync(() -> this.deleteDatabase(name));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> containsDatabaseAsync(@NonNull String name) {
    return TaskUtil.supplyAsync(() -> this.containsDatabase(name));
  }
}
