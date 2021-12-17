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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler;

import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultRPCHandlerRegistry implements RPCHandlerRegistry {

  protected final Map<String, RPCHandler> handlers = new ConcurrentHashMap<>();

  @Override
  public @NonNull @UnmodifiableView Map<String, RPCHandler> registeredHandlers() {
    return Collections.unmodifiableMap(this.handlers);
  }

  @Override
  public boolean hasHandler(@NonNull Class<?> targetClass) {
    return this.hasHandler(targetClass.getCanonicalName());
  }

  @Override
  public boolean hasHandler(@NonNull String targetClassName) {
    return this.handlers.containsKey(targetClassName);
  }

  @Override
  public @Nullable RPCHandler handler(@NonNull Class<?> targetClass) {
    return this.handler(targetClass.getCanonicalName());
  }

  @Override
  public @Nullable RPCHandler handler(@NonNull String targetClassName) {
    return this.handlers.get(targetClassName);
  }

  @Override
  public boolean registerHandler(@NonNull RPCHandler rpcHandler) {
    return this.handlers.put(rpcHandler.targetClass().getCanonicalName(), rpcHandler) == null;
  }

  @Override
  public boolean unregisterHandler(@NonNull RPCHandler rpcHandler) {
    return this.unregisterHandler(rpcHandler.targetClass());
  }

  @Override
  public boolean unregisterHandler(@NonNull Class<?> rpcHandlerTargetClass) {
    return this.unregisterHandler(rpcHandlerTargetClass.getCanonicalName());
  }

  @Override
  public boolean unregisterHandler(@NonNull String rpcHandlerTargetClassName) {
    return this.handlers.remove(rpcHandlerTargetClassName) != null;
  }

  @Override
  public void unregisterHandlers(@NonNull ClassLoader classLoader) {
    for (var entry : this.handlers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(classLoader)) {
        this.handlers.remove(entry.getKey(), entry.getValue());
      }
    }
  }
}
