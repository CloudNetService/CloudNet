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

package eu.cloudnetservice.cloudnet.wrapper.database;

import eu.cloudnetservice.cloudnet.driver.database.Database;
import eu.cloudnetservice.cloudnet.driver.database.DatabaseProvider;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import lombok.NonNull;

public class DefaultWrapperDatabaseProvider implements DatabaseProvider {

  private final Wrapper wrapper;
  private final RPCSender rpcSender;

  public DefaultWrapperDatabaseProvider(@NonNull Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      DatabaseProvider.class);
  }

  @Override
  public @NonNull Database database(@NonNull String name) {
    return new WrapperDatabase(name, this.wrapper, this.rpcSender.invokeMethod("database", name));
  }

  @Override
  public boolean containsDatabase(@NonNull String name) {
    return this.rpcSender.invokeMethod("containsDatabase", name).fireSync();
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    return this.rpcSender.invokeMethod("deleteDatabase", name).fireSync();
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    return this.rpcSender.invokeMethod("databaseNames").fireSync();
  }
}
