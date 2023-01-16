/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.defaults.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Defaults;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.RPCInvocationContext;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.defaults.MethodInformation;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the default implementation of a rpc handler.
 *
 * @since 4.0
 */
public class DefaultRPCHandler extends DefaultRPCProvider implements RPCHandler {

  protected final Class<?> bindingClass;
  protected final Object bindingInstance;
  protected final MethodInvokerGenerator generator;

  protected final Cache<String, MethodInformation> methodCache = Caffeine.newBuilder().build();

  /**
   * Constructs a new default rpc handler instance.
   *
   * @param clazz          the raw clazz to which the handler is bound.
   * @param binding        the instance to which the handler is bound, might be null if not needed.
   * @param objectMapper   the object mapper used for reading / writing of arguments and return values.
   * @param dataBufFactory the data buf factory used to allocate response buffers.
   * @throws NullPointerException if either the given class, object mapper or buffer factory is null.
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerTo(@NonNull RPCHandlerRegistry registry) {
    registry.registerHandler(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HandlingResult handle(@NonNull RPCInvocationContext context) {
    // get the working instance
    var inst = context.workingInstance();
    if (inst == null) {
      inst = context.strictInstanceUsage() ? null : this.bindingInstance;
    }
    // now we try to find the associated method information to the given method name or try to read it
    var instance = inst; // pail
    var information = this.methodCache.get(
      String.format(
        "%d@%s@%s@%d",
        inst == null ? -1 : inst.hashCode(),
        this.bindingClass.getCanonicalName(),
        context.methodName(),
        context.argumentCount()),
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
