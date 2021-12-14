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

package de.dytanic.cloudnet.database.xodus;

import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.LocalDatabase;
import de.dytanic.cloudnet.database.util.LocalDatabaseUtils;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import org.jetbrains.annotations.NotNull;

public class XodusDatabaseProvider extends AbstractDatabaseProvider {

  protected final boolean runsInCluster;

  protected final File databaseDirectory;
  protected final ExecutorService executorService;
  protected final boolean autoShutdownExecutorService;
  protected final Map<String, LocalDatabase> cachedDatabaseInstances;

  protected final EnvironmentConfig environmentConfig;

  protected Environment environment;

  public XodusDatabaseProvider(File databaseDirectory, boolean runsInCluster) {
    this(databaseDirectory, runsInCluster, null);
  }

  public XodusDatabaseProvider(File databaseDirectory, boolean runsInCluster, ExecutorService executorService) {
    this.runsInCluster = runsInCluster;
    this.databaseDirectory = databaseDirectory;
    this.autoShutdownExecutorService = executorService == null;
    this.executorService = executorService == null ? Executors.newCachedThreadPool() : executorService;
    this.cachedDatabaseInstances = new ConcurrentHashMap<>();

    this.environmentConfig = new EnvironmentConfig()
      .setLogCacheShared(true)
      .setEnvCloseForcedly(true)
      .setEnvGatherStatistics(false)
      .setEnvTxnDowngradeAfterFlush(true);
  }

  @Override
  public boolean init() {
    if (this.runsInCluster) {
      LocalDatabaseUtils.bigWarningThatEveryoneCanSee(I18n.trans("cloudnet-cluster-local-db-warning"));
    }

    this.environment = Environments.newInstance(this.databaseDirectory, this.environmentConfig);
    return true;
  }

  @Override
  public @NotNull LocalDatabase getDatabase(@NotNull String name) {
    return this.cachedDatabaseInstances.computeIfAbsent(name, $ -> this.environment.computeInTransaction(txn -> {
      var store = this.environment.openStore(name, StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, txn);
      return new XodusDatabase(name, this.executorService, store, this);
    }));
  }

  @Override
  public boolean containsDatabase(@NotNull String name) {
    return this.environment.computeInReadonlyTransaction(txn -> this.environment.storeExists(name, txn));
  }

  @Override
  public boolean deleteDatabase(@NotNull String name) {
    this.cachedDatabaseInstances.remove(name);
    this.environment.executeInTransaction(txn -> this.environment.removeStore(name, txn));

    return true;
  }

  @Override
  public @NotNull Collection<String> getDatabaseNames() {
    return this.environment.computeInReadonlyTransaction(txn -> this.environment.getAllStoreNames(txn));
  }

  @Override
  public void close() {
    this.environment.close();
    this.cachedDatabaseInstances.clear();

    if (this.autoShutdownExecutorService) {
      this.executorService.shutdownNow();
    }
  }

  @Override
  public @NotNull String getName() {
    return "xodus";
  }
}
