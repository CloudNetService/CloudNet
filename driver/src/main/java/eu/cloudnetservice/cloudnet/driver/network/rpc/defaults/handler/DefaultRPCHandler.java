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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler;

import com.google.common.base.Defaults;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCInvocationContext;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.MethodInformation;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPCHandler extends DefaultRPCProvider implements RPCHandler {

  protected final Class<?> bindingClass;
  protected final Object bindingInstance;
  protected final MethodInvokerGenerator generator;

  protected final Map<String, MethodInformation> methodCache = new ConcurrentHashMap<>();

  public DefaultRPCHandler(
    @NonNull Class<?> clazz,
    @Nullable Object binding,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    super(clazz, objectMapper, dataBufFactory);

    this.bindingClass = clazz;
    this.bindingInstance = binding;
    this.generator = new MethodInvokerGenerator();
  }

  @Override
  public void registerToDefaultRegistry() {
    this.registerTo(CloudNetDriver.instance().rpcHandlerRegistry());
  }

  @Override
  public void registerTo(@NonNull RPCHandlerRegistry registry) {
    registry.registerHandler(this);
  }

  @Override
  public @NonNull HandlingResult handle(@NonNull RPCInvocationContext context) {
    // get the working instance
    var instance = context
      .workingInstance()
      .orElse(context.strictInstanceUsage() ? null : this.bindingInstance);
    // now we try to find the associated method information to the given method name or try to read it
    var information = this.methodCache.computeIfAbsent(
      String.format("%d@%s", instance == null ? -1 : instance.hashCode(), context.methodName()),
      $ -> MethodInformation.find(
        instance,
        this.bindingClass,
        context.methodName(),
        instance == null ? null : this.generator,
        context.argumentCount()));
    // now as we have the method info, try to read all arguments needed
    var arguments = new Object[information.arguments().length];
    for (var i = 0; i < arguments.length; i++) {
      arguments[i] = this.objectMapper.readObject(context.argumentInformation(), information.arguments()[i]);
    }
    // get the method invocation result
    HandlingResult result;
    if (instance == null) {
      // no instance provided, no invocation we can make - just check if the result is primitive and return the default
      // primitive value associated
      if (information.rawReturnType().isPrimitive() && context.normalizePrimitives()) {
        result = DefaultHandlingResult.success(
          information,
          this,
          Defaults.defaultValue(information.rawReturnType()));
      } else {
        // no instance and not primitive means null
        result = DefaultHandlingResult.success(information, this, null);
      }
    } else {
      // there is an instance we can work on, do it!
      try {
        // spare the result allocation if the method invocation fails
        var methodResult = information.methodInvoker().callMethod(arguments);
        // now we can create the result as the method invocation succeeded
        result = DefaultHandlingResult.success(information, this, methodResult);
      } catch (Throwable throwable) {
        // an exception occurred when invoking the method - not good
        result = DefaultHandlingResult.failure(information, this, throwable);
      }
    }
    // return the result
    return result;
  }
}
