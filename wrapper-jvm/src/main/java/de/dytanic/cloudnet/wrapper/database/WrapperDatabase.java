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

package de.dytanic.cloudnet.wrapper.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public class WrapperDatabase implements Database {

  private final String name;
  private final RPC baseRPC;
  private final RPCSender sender;

  public WrapperDatabase(@NonNull String name, @NonNull Wrapper wrapper, @NonNull RPC baseRPC) {
    this.name = name;
    this.baseRPC = baseRPC;
    this.sender = wrapper.rpcProviderFactory().providerForClass(wrapper.networkClient(), Database.class);
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull JsonDocument document) {
    return this.baseRPC.join(this.sender.invokeMethod("insert", key, document)).fireSync();
  }

  @Override
  public boolean update(@NonNull String key, @NonNull JsonDocument document) {
    return this.baseRPC.join(this.sender.invokeMethod("update", key, document)).fireSync();
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.baseRPC.join(this.sender.invokeMethod("contains", key)).fireSync();
  }

  @Override
  public boolean delete(@NonNull String key) {
    return this.baseRPC.join(this.sender.invokeMethod("delete", key)).fireSync();
  }

  @Override
  public JsonDocument get(String key) {
    return this.baseRPC.join(this.sender.invokeMethod("get", key)).fireSync();
  }

  @Override
  public @NonNull List<JsonDocument> get(@NonNull String fieldName, Object fieldValue) {
    return this.baseRPC.join(this.sender.invokeMethod("get", fieldName, fieldValue)).fireSync();
  }

  @Override
  public @NonNull List<JsonDocument> get(@NonNull JsonDocument filters) {
    return this.baseRPC.join(this.sender.invokeMethod("get", filters)).fireSync();
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.baseRPC.join(this.sender.invokeMethod("keys")).fireSync();
  }

  @Override
  public @NonNull Collection<JsonDocument> documents() {
    return this.baseRPC.join(this.sender.invokeMethod("documents")).fireSync();
  }

  @Override
  public @NonNull Map<String, JsonDocument> entries() {
    return this.baseRPC.join(this.sender.invokeMethod("entries")).fireSync();
  }

  @Override
  public void clear() {
    this.baseRPC.join(this.sender.invokeMethod("clear")).fireSync();
  }

  @Override
  public long documentCount() {
    return this.baseRPC.join(this.sender.invokeMethod("documentCount")).fireSync();
  }

  @Override
  public boolean synced() {
    return this.baseRPC.join(this.sender.invokeMethod("synced")).fireSync();
  }

  @Override
  public void close() {
    this.baseRPC.join(this.sender.invokeMethod("close")).fireSync();
  }
}
