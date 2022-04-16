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

package eu.cloudnetservice.wrapper.database;

import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import lombok.NonNull;

public abstract class DefaultWrapperDatabaseProvider implements DatabaseProvider {

  private final RPCSender rpcSender;

  public DefaultWrapperDatabaseProvider(@NonNull RPCSender sender) {
    this.rpcSender = sender;
  }

  @Override
  public @NonNull Database database(@NonNull String name) {

    // return ChainedFactory.generate(...).newInstance(name)

    // WrapperDatabaseImpl(RPC base, ...)
    //  this.base = base;
    //  super(...)


    return new WrapperDatabase(name, this.rpcSender.invokeMethod("database", name));
  }

  public abstract static class WrapperDatabase2 {
    private final String name;

    protected WrapperDatabase2(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }
  }

  public static class WrapperDatabaseImpl extends WrapperDatabase2 {

    private final RPC base;
    private final RPCSender sender;

    public WrapperDatabaseImpl(RPCSender sender, RPC base, String name) {
      super(name);
      this.base = base;
      this.sender = sender;
    }
  }
}

