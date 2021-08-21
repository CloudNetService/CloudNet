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

package de.dytanic.cloudnet.driver.network.rpc;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface RPCHandlerRegistry {

  @NotNull
  @UnmodifiableView Map<String, RPCHandler> getRegisteredHandlers();

  boolean hasHandler(@NotNull Class<?> targetClass);

  boolean hasHandler(@NotNull String targetClassName);

  @Nullable RPCHandler getHandler(@NotNull Class<?> targetClass);

  @Nullable RPCHandler getHandler(@NotNull String targetClassName);

  boolean registerHandler(@NotNull RPCHandler rpcHandler);

  boolean unregisterHandler(@NotNull RPCHandler rpcHandler);

  boolean unregisterHandler(@NotNull Class<?> rpcHandlerTargetClass);

  boolean unregisterHandler(@NotNull String rpcHandlerTargetClassName);

  void unregisterHandlers(@NotNull ClassLoader classLoader);
}
