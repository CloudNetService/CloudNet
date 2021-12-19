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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPCSender extends DefaultRPCProvider implements RPCSender {

  protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  protected final Class<?> targetClass;
  protected final RPCProviderFactory factory;
  protected final INetworkComponent networkComponent;
  protected final Map<String, MethodInformation> cachedMethodInformation;

  public DefaultRPCSender(
    @NonNull RPCProviderFactory factory,
    @Nullable INetworkComponent component,
    @NonNull Class<?> targetClass,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    super(targetClass, objectMapper, dataBufFactory);

    this.factory = factory;
    this.targetClass = targetClass;
    this.networkComponent = component;
    this.cachedMethodInformation = new ConcurrentHashMap<>();
  }

  @Override
  public @NonNull RPCProviderFactory factory() {
    return this.factory;
  }

  @Override
  public @NonNull INetworkComponent associatedComponent() {
    // possible to create without an associated component - throw an exception if so
    if (this.networkComponent == null) {
      throw new UnsupportedOperationException("Sender has no associated component");
    }
    return this.networkComponent;
  }

  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName) {
    return this.invokeMethod(methodName, EMPTY_OBJECT_ARRAY);
  }

  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName, Object... args) {
    // find the method information of the method we want to invoke
    var information = this.cachedMethodInformation.computeIfAbsent(
      methodName,
      $ -> MethodInformation.find(null, this.targetClass, methodName, null, args.length));
    // generate the rpc from this information
    return new DefaultRPC(
      this,
      this.targetClass,
      methodName,
      args,
      this.objectMapper,
      information.returnType(),
      this.dataBufFactory);
  }
}
