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

package de.dytanic.cloudnet.cluster.sync;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultDataSyncRegistry implements DataSyncRegistry {

  private final Map<String, DataSyncHandler<?>> handlers = new ConcurrentHashMap<>();

  @Override
  public void registerHandler(@NotNull DataSyncHandler<?> handler) {
    this.handlers.putIfAbsent(handler.getKey(), handler);
  }

  @Override
  public void unregisterHandler(@NotNull DataSyncHandler<?> handler) {
    this.handlers.remove(handler.getKey());
  }

  @Override
  public void unregisterHandler(@NotNull String handlerKey) {
    this.handlers.remove(handlerKey);
  }

  @Override
  public void unregisterHandler(@NotNull ClassLoader loader) {
    for (Entry<String, DataSyncHandler<?>> entry : this.handlers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(loader)) {
        this.handlers.remove(entry.getKey());
        break;
      }
    }
  }

  @Override
  public boolean hasHandler(@NotNull String handlerKey) {
    return this.handlers.containsKey(handlerKey);
  }

  @Override
  public @Nullable DataBuf handle(@NotNull DataBuf input, boolean force) {
    return null;
  }

  @Override
  public @NotNull DataBuf.Mutable prepareClusterData(@NotNull DataBuf.Mutable to, boolean force) {
    return null;
  }
}
