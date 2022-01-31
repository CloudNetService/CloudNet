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

package eu.cloudnetservice.cloudnet.node.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.driver.database.DatabaseProvider;
import java.time.Duration;
import lombok.NonNull;

public abstract class AbstractDatabaseProvider implements DatabaseProvider, Nameable, AutoCloseable {

  protected final Cache<String, LocalDatabase> databaseCache = Caffeine.newBuilder()
    .scheduler(Scheduler.systemScheduler())
    .expireAfterAccess(Duration.ofMinutes(5))
    .build();
  protected DatabaseHandler databaseHandler = new DefaultDatabaseHandler();

  public abstract boolean init() throws Exception;

  public @NonNull DatabaseHandler databaseHandler() {
    return this.databaseHandler;
  }

  public void databaseHandler(@NonNull DatabaseHandler databaseHandler) {
    this.databaseHandler = databaseHandler;
  }

  public @NonNull Cache<String, LocalDatabase> databaseCache() {
    return this.databaseCache;
  }

  @Override
  public void close() throws Exception {
    this.databaseCache.invalidateAll();
  }

  @Override
  public abstract @NonNull LocalDatabase database(@NonNull String name);
}
