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

import com.google.common.base.Defaults;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCInvocationContext;
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
    @Nullable Object binding,
    @NotNull ObjectMapper objectMapper,
    @NotNull DataBufFactory dataBufFactory
  ) {
    super(clazz, objectMapper, dataBufFactory);

    this.bindingClass = clazz;
    this.bindingInstance = binding;
    this.generator = new MethodInvokerGenerator();
  }

  @Override
  public @NotNull Pair<Object, MethodInformation> handle(@NotNull RPCInvocationContext context) {
    // get the working instance
    Object instance = context
      .getWorkingInstance()
      .orElse(context.strictInstanceUsage() ? null : this.bindingInstance);
    // now we try to find the associated method information to the given method name or try to read it
    MethodInformation information = this.methodCache.computeIfAbsent(
      String.format("%d@%s", instance == null ? -1 : instance.hashCode(), context.getMethodName()),
      $ -> MethodInformation.find(
        instance,
        this.bindingClass,
        context.getMethodName(),
        instance == null ? null : this.generator));
    // now as we have the method info, try to read all arguments needed
    Object[] arguments = new Object[information.getArguments().length];
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = this.objectMapper.readObject(context.getArgumentInformation(), information.getArguments()[i]);
    }
    // invoke the method in the target class
    Object result = instance == null ? null : information.getMethodInvoker().callMethod(arguments);
    // check if we need to normalize if the result is a primitive
    if (result == null && information.getRawReturnType().isPrimitive() && context.normalizePrimitives()) {
      result = Defaults.defaultValue(information.getRawReturnType());
    }
    // return the result
    return new Pair<>(result, information);
  }
}
