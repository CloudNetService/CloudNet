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

package de.dytanic.cloudnet.driver.network.rpc.defaults;

import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.defaults.sender.DefaultRPCSender;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPCProviderFactory implements RPCProviderFactory {

  protected final ObjectMapper defaultObjectMapper;
  protected final DataBufFactory defaultDataBufFactory;

  public DefaultRPCProviderFactory(ObjectMapper defaultObjectMapper, DataBufFactory defaultDataBufFactory) {
    this.defaultObjectMapper = defaultObjectMapper;
    this.defaultDataBufFactory = defaultDataBufFactory;
  }

  @Override
  public @NotNull ObjectMapper getDefaultObjectMapper() {
    return this.defaultObjectMapper;
  }

  @Override
  public @NotNull DataBufFactory getDefaultDataBufFactory() {
    return this.defaultDataBufFactory;
  }

  @Override
  public @NotNull RPCSender providerForClass(@NotNull INetworkComponent component, @NotNull Class<?> clazz) {
    return this.providerForClass(component, clazz, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  @Override
  public @NotNull RPCSender providerForClass(
    @NotNull INetworkComponent component,
    @NotNull Class<?> clazz,
    @NotNull ObjectMapper objectMapper,
    @NotNull DataBufFactory dataBufFactory
  ) {
    return new DefaultRPCSender(component, clazz, objectMapper, dataBufFactory);
  }

  @Override
  public @NotNull RPCHandler newHandler(@NotNull Class<?> clazz, @Nullable Object binding) {
    return this.newHandler(clazz, binding, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  @Override
  public @NotNull RPCHandler newHandler(
    @NotNull Class<?> clazz,
    @Nullable Object binding,
    @NotNull ObjectMapper objectMapper,
    @NotNull DataBufFactory dataBufFactory
  ) {
    return new DefaultRPCHandler(clazz, binding, objectMapper, dataBufFactory);
  }
}
