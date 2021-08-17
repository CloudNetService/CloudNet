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
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProvider;
import de.dytanic.cloudnet.driver.network.rpc.defaults.MethodInformation;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPCHandler extends DefaultRPCProvider implements RPCHandler {

  protected final Class<?> bindingClass;
  protected final Object bindingInstance;
  protected final MethodInvokerGenerator generator;

  protected final Map<String, MethodInformation> methodCache = new ConcurrentHashMap<>();

  public DefaultRPCHandler(
    @NotNull Class<?> clazz,
    @NotNull Object binding,
    @NotNull ObjectMapper objectMapper,
    @NotNull DataBufFactory dataBufFactory
  ) {
    super(clazz, objectMapper, dataBufFactory);

    this.bindingClass = clazz;
    this.bindingInstance = binding;
    this.generator = new MethodInvokerGenerator();
  }

  @Override
  public @Nullable DataBuf handleRPC(@NotNull INetworkChannel channel, @NotNull DataBuf received) {
    // read contract (the class name to which the rpc points is already gone from the buffer)
    String targetMethodName = received.readString();
    boolean expectsMethodResult = received.readBoolean();
    // now we try to find the associated method information to the given method name or try to read it
    MethodInformation information = this.methodCache.computeIfAbsent(
      targetMethodName,
      $ -> MethodInformation.find(this.bindingInstance, this.bindingClass, targetMethodName, this.generator));
    // now as we have the method info, try to read all arguments needed
    Object[] arguments = new Object[information.getArguments().length];
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = this.objectMapper.readObject(received, information.getArguments()[i]);
    }
    // invoke the method in the target class
    Object result = information.getMethodInvoker().callMethod(arguments);
    if (!expectsMethodResult) {
      // break here, we don't need to write something
      return null;
    }
    // check if the method is a void method, if so just write null into the buffer and send
    if (information.isVoidMethod()) {
      return this.dataBufFactory.createWithExpectedSize(1).writeBoolean(false);
    }
    // write the result into the buffer
    return this.objectMapper.writeObject(this.dataBufFactory.createEmpty(), result);
  }
}
