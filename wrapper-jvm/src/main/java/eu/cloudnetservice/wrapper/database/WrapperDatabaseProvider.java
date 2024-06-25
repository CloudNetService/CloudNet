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

package eu.cloudnetservice.wrapper.database;

import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import lombok.NonNull;

public abstract class WrapperDatabaseProvider implements DatabaseProvider {

  private final RPCSender providerRPCSender;
  private final RPCImplementationBuilder.InstanceAllocator<? extends Database> databaseImplAllocator;

  @RPCInvocationTarget
  public WrapperDatabaseProvider(@NonNull RPCSender sender) {
    this.providerRPCSender = sender;

    var rpcFactory = sender.sourceFactory();
    this.databaseImplAllocator = rpcFactory.newRPCBasedImplementationBuilder(WrapperDatabase.class)
      .targetChannel(sender.fallbackChannelSupplier())
      .generateImplementation();
  }

  @Override
  public @NonNull Database database(@NonNull String name) {
    var baseRPC = this.providerRPCSender.invokeCaller(name);
    return this.databaseImplAllocator
      .withBaseRPC(baseRPC)
      .withAdditionalConstructorParameters(name)
      .allocate();
  }
}
