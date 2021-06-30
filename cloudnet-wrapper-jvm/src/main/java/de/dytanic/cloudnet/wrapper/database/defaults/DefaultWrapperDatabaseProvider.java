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

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.RemoteDatabaseRequestType;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.wrapper.database.IDatabase;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;
import de.dytanic.cloudnet.wrapper.network.packet.PacketClientDatabaseAction;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class DefaultWrapperDatabaseProvider implements IDatabaseProvider {

  @Override
  public IDatabase getDatabase(String name) {
    return new WrapperDatabase(name, this);
  }

  @Override
  public boolean containsDatabase(String name) {
    return this.containsDatabaseAsync(name).getDef(false);
  }

  @Override
  public boolean deleteDatabase(String name) {
    return this.deleteDatabaseAsync(name).getDef(false);
  }

  @Override
  public Collection<String> getDatabaseNames() {
    return this.getDatabaseNamesAsync().getDef(Collections.emptyList());
  }

  @Override
  @NotNull
  public ITask<Boolean> containsDatabaseAsync(String name) {
    return this.executeQuery(RemoteDatabaseRequestType.CONTAINS_DATABASE, buffer -> buffer.writeString(name))
      .map(packet -> packet.getBuffer().readBoolean());
  }

  @Override
  @NotNull
  public ITask<Boolean> deleteDatabaseAsync(String name) {
    return this.executeQuery(RemoteDatabaseRequestType.DELETE_DATABASE, buffer -> buffer.writeString(name))
      .map(packet -> packet.getBuffer().readBoolean());
  }

  @Override
  @NotNull
  public ITask<Collection<String>> getDatabaseNamesAsync() {
    return this.executeQuery(RemoteDatabaseRequestType.GET_DATABASES, null)
      .map(packet -> packet.getBuffer().readStringCollection());
  }

  ITask<IPacket> executeQuery(RemoteDatabaseRequestType requestType, Consumer<ProtocolBuffer> modifier) {
    return CloudNetDriver.getInstance().getNetworkClient().getFirstChannel()
      .sendQueryAsync(new PacketClientDatabaseAction(requestType, modifier));
  }

}
