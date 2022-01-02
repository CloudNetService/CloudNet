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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults;

import eu.cloudnetservice.cloudnet.driver.network.NetworkComponent;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCProviderFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.sender.DefaultRPCSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPCProviderFactory implements RPCProviderFactory {

  protected final ObjectMapper defaultObjectMapper;
  protected final DataBufFactory defaultDataBufFactory;

  public DefaultRPCProviderFactory(ObjectMapper defaultObjectMapper, DataBufFactory defaultDataBufFactory) {
    this.defaultObjectMapper = defaultObjectMapper;
    this.defaultDataBufFactory = defaultDataBufFactory;
  }

  @Override
  public @NonNull ObjectMapper defaultObjectMapper() {
    return this.defaultObjectMapper;
  }

  @Override
  public @NonNull DataBufFactory defaultDataBufFactory() {
    return this.defaultDataBufFactory;
  }

  @Override
  public @NonNull RPCSender providerForClass(@Nullable NetworkComponent component, @NonNull Class<?> clazz) {
    return this.providerForClass(component, clazz, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  @Override
  public @NonNull RPCSender providerForClass(
    @Nullable NetworkComponent component,
    @NonNull Class<?> clazz,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    return new DefaultRPCSender(this, component, clazz, objectMapper, dataBufFactory);
  }

  @Override
  public @NonNull RPCHandler newHandler(@NonNull Class<?> clazz, @Nullable Object binding) {
    return this.newHandler(clazz, binding, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  @Override
  public @NonNull RPCHandler newHandler(
    @NonNull Class<?> clazz,
    @Nullable Object binding,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    return new DefaultRPCHandler(clazz, binding, objectMapper, dataBufFactory);
  }
}
