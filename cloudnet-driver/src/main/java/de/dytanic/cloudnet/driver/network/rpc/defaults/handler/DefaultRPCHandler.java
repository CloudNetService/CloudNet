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

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProvider;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public class DefaultRPCHandler extends DefaultRPCProvider implements RPCHandler {

  protected final Class<?> bindingClass;
  protected final Object bindingInstance;
  protected final MethodInvokerGenerator generator;

  protected final Map<String, MethodInformation> methodCache = new ConcurrentHashMap<>();

  public DefaultRPCHandler(@NotNull Class<?> clazz, @NotNull Object binding, @NotNull ObjectMapper objectMapper) {
    super(clazz.getCanonicalName(), objectMapper);

    this.bindingClass = clazz;
    this.bindingInstance = binding;
    this.generator = new MethodInvokerGenerator();
  }

  @Override
  public @NotNull DataBuf handleRPC(@NotNull INetworkChannel channel, @NotNull DataBuf received) {
    // read contract (the class name to which the rpc points is already gone from the buffer)
    String targetMethodName = received.readString();
    boolean expectsMethodResult = received.readBoolean();
    // now we try to find the associated method information to the given method name or try to read it
    MethodInformation information = this.methodCache.computeIfAbsent(
      targetMethodName,
      $ -> MethodInformation.find(this.bindingInstance, targetMethodName, this.generator));
    //

    return null;
  }
}
