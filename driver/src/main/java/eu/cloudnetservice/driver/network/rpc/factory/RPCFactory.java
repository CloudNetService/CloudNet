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

package eu.cloudnetservice.driver.network.rpc.factory;

import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import lombok.NonNull;

/**
 * A factory which can provide anything which is related to rpc.
 *
 * @since 4.0
 */
public interface RPCFactory {

  @NonNull
  RPCSender.Builder newRPCSenderBuilder(@NonNull Class<?> target);

  @NonNull
  <T> RPCHandler.Builder<T> newRPCHandlerBuilder(@NonNull Class<T> target);

  @NonNull
  <T> RPCImplementationBuilder<T> newPCBasedImplementationBuilder(@NonNull Class<T> baseClass);
}
