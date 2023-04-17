/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import java.time.Duration;
import lombok.NonNull;

public abstract class NodeDatabaseProvider implements DatabaseProvider, Named, AutoCloseable {

  protected static final Logger LOGGER = LogManager.logger(NodeDatabaseProvider.class);
  protected static final RemovalListener<String, LocalDatabase> DEFAULT_REMOVAL_LISTENER = (key, value, cause) -> {
    // close the database instance that was removed, unless the database instance was garbage collected
    if (value != null) {
      try {
        value.close();
      } catch (Exception exception) {
        LOGGER.severe("Exception closing removed database instance %s", exception, value.name());
      }
    }
  };

  protected final Cache<String, LocalDatabase> databaseCache;

  protected NodeDatabaseProvider(@NonNull RemovalListener<String, LocalDatabase> removalListener) {
    this.databaseCache = Caffeine.newBuilder()
      .scheduler(Scheduler.systemScheduler())
      .expireAfterAccess(Duration.ofMinutes(5))
      .removalListener(removalListener)
      .build();
  }

  public abstract boolean init() throws Exception;

  @Override
  public abstract @NonNull LocalDatabase database(@NonNull String name);

  @Override
  public void close() throws Exception {
    this.databaseCache.invalidateAll();
  }
}
