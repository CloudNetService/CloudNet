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

package de.dytanic.cloudnet.driver.network.rpc;

import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface RPCHandlerRegistry {

  @NonNull
  @UnmodifiableView Map<String, RPCHandler> registeredHandlers();

  boolean hasHandler(@NonNull Class<?> targetClass);

  boolean hasHandler(@NonNull String targetClassName);

  @Nullable RPCHandler handler(@NonNull Class<?> targetClass);

  @Nullable RPCHandler handler(@NonNull String targetClassName);

  boolean registerHandler(@NonNull RPCHandler rpcHandler);

  boolean unregisterHandler(@NonNull RPCHandler rpcHandler);

  boolean unregisterHandler(@NonNull Class<?> rpcHandlerTargetClass);

  boolean unregisterHandler(@NonNull String rpcHandlerTargetClassName);

  void unregisterHandlers(@NonNull ClassLoader classLoader);
}
