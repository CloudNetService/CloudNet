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

import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RPCProviderFactory {

  @NotNull ObjectMapper getDefaultObjectMapper();

  @NotNull DataBufFactory getDefaultDataBufFactory();

  @NotNull RPCSender providerForClass(@NotNull INetworkComponent component, @NotNull Class<?> clazz);

  @NotNull RPCSender providerForClass(@NotNull INetworkComponent component, @NotNull Class<?> clazz,
    @NotNull ObjectMapper objectMapper, @NotNull DataBufFactory dataBufFactory);

  @NotNull RPCHandler newHandler(@NotNull Class<?> clazz, @Nullable Object binding);

  @NotNull RPCHandler newHandler(@NotNull Class<?> clazz, @Nullable Object binding,
    @NotNull ObjectMapper objectMapper, @NotNull DataBufFactory dataBufFactory);
}
