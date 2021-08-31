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

package de.dytanic.cloudnet.driver.network.rpc.defaults.sender;

import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProvider;
import de.dytanic.cloudnet.driver.network.rpc.defaults.MethodInformation;
import de.dytanic.cloudnet.driver.network.rpc.defaults.rpc.DefaultRPC;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public class DefaultRPCSender extends DefaultRPCProvider implements RPCSender {

  protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  protected final Class<?> targetClass;
  protected final RPCProviderFactory factory;
  protected final INetworkComponent networkComponent;
  protected final Map<String, MethodInformation> cachedMethodInformation;

  public DefaultRPCSender(
    @NotNull RPCProviderFactory factory,
    @NotNull INetworkComponent component,
    @NotNull Class<?> targetClass,
    @NotNull ObjectMapper objectMapper,
    @NotNull DataBufFactory dataBufFactory
  ) {
    super(targetClass, objectMapper, dataBufFactory);

    this.factory = factory;
    this.targetClass = targetClass;
    this.networkComponent = component;
    this.cachedMethodInformation = new ConcurrentHashMap<>();
  }

  @Override
  public @NotNull RPCProviderFactory getFactory() {
    return this.factory;
  }

  @Override
  public @NotNull INetworkComponent getAssociatedComponent() {
    return this.networkComponent;
  }

  @Override
  public @NotNull RPC invokeMethod(@NotNull String methodName) {
    return this.invokeMethod(methodName, EMPTY_OBJECT_ARRAY);
  }

  @Override
  public @NotNull RPC invokeMethod(@NotNull String methodName, Object... args) {
    // find the method information of the method we want to invoke
    MethodInformation information = this.cachedMethodInformation.computeIfAbsent(
      methodName,
      $ -> MethodInformation.find(null, this.targetClass, methodName, null));
    // generate the rpc from this information
    return new DefaultRPC(
      this,
      this.targetClass,
      methodName,
      args,
      this.objectMapper,
      information.getReturnType(),
      this.dataBufFactory);
  }
}
