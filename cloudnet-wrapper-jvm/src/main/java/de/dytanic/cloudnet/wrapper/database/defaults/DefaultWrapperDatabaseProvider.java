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

package de.dytanic.cloudnet.wrapper.database.defaults;

import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;
import java.util.Collection;

public class DefaultWrapperDatabaseProvider implements IDatabaseProvider {

  private final Wrapper wrapper;
  private final RPCSender rpcSender;

  public DefaultWrapperDatabaseProvider(Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), DatabaseProvider.class);
  }

  @Override
  public Database getDatabase(String name) {
    return new WrapperDatabase(name, this, this.wrapper);
  }

  @Override
  public boolean containsDatabase(String name) {
    return this.rpcSender.invokeMethod("containsDatabase", name).fireSync();
  }

  @Override
  public boolean deleteDatabase(String name) {
    return this.rpcSender.invokeMethod("deleteDatabase", name).fireSync();
  }

  @Override
  public Collection<String> getDatabaseNames() {
    return this.rpcSender.invokeMethod("getDatabaseNames").fireSync();
  }
}
