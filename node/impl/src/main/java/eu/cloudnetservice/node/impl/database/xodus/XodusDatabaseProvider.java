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

package eu.cloudnetservice.node.impl.database.xodus;

import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.AbstractNodeDatabaseProvider;
import eu.cloudnetservice.node.impl.database.util.LocalDatabaseUtil;
import java.io.File;
import java.util.Collection;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.StoreConfig;
import lombok.NonNull;

public class XodusDatabaseProvider extends AbstractNodeDatabaseProvider {

  protected final I18n i18n;
  protected final boolean runsInCluster;
  protected final File databaseDirectory;

  protected final EnvironmentConfig environmentConfig;

  protected Environment environment;

  public XodusDatabaseProvider(@NonNull @Service I18n i18n, @NonNull File databaseDirectory, boolean runsInCluster) {
    super(DEFAULT_REMOVAL_LISTENER);

    this.i18n = i18n;
    this.runsInCluster = runsInCluster;
    this.databaseDirectory = databaseDirectory;

    this.environmentConfig = new EnvironmentConfig()
      .setLogCacheShared(true)
      .setEnvCloseForcedly(true)
      .setEnvGatherStatistics(false)
      .setEnvTxnDowngradeAfterFlush(true);
  }

  @Override
  public boolean init() {
    if (this.runsInCluster) {
      LocalDatabaseUtil.bigWarningThatEveryoneCanSee(this.i18n.translate("cluster-local-db-warning"));
    }

    this.environment = Environments.newInstance(this.databaseDirectory, this.environmentConfig);
    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, $ -> this.environment.computeInTransaction(txn -> {
      var store = this.environment.openStore(name, StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, txn);
      return new XodusDatabase(name, store, this);
    }));
  }

  @Override
  public boolean containsDatabase(@NonNull String name) {
    return this.environment.computeInReadonlyTransaction(txn -> this.environment.storeExists(name, txn));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    this.databaseCache.invalidate(name);
    this.environment.executeInTransaction(txn -> this.environment.removeStore(name, txn));

    return true;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    return this.environment.computeInReadonlyTransaction(txn -> this.environment.getAllStoreNames(txn));
  }

  @Override
  public void close() throws Exception {
    super.close();
    this.environment.close();
  }

  @Override
  public @NonNull String name() {
    return "xodus";
  }
}
