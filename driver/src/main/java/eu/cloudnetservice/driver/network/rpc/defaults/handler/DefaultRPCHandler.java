/*
 * Copyright 2019-2024 CloudNetService team & contributors
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
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationContext;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import io.vavr.control.Try;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.TypeDescriptor;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the default implementation of a rpc handler.
 *
 * @since 4.0
 */
public class DefaultRPCHandler extends DefaultRPCProvider implements RPCHandler {

  private static final Logger LOGGER = LogManager.logger(DefaultRPCHandler.class);

  protected final Object boundInstance;
  protected final RPCClassMetadata targetClassMeta;
  protected final Cache<RPCMethodMetadata, MethodInvoker> methodInvokerCache;

  public DefaultRPCHandler(
    @NonNull Class<?> targetClass,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory,
    @Nullable Object boundInstance,
    @NonNull RPCClassMetadata targetClassMeta
  ) {
    super(targetClass, objectMapper, dataBufFactory);
    this.boundInstance = boundInstance;
    this.targetClassMeta = targetClassMeta;
    this.methodInvokerCache = Caffeine.newBuilder()
      // remove invokers from cache after idling some time as they are defined as nested classes
      // which means that they can get garbage collected to free up some heap when there are
      // no more strong references (like the cache) to them anymore
      .expireAfterAccess(Duration.ofDays(1))
      .build();
  }

  /**
   * Tries to parse the given method descriptor, returning null if the given method descriptor is invalid.
   *
   * @param descriptor the descriptor that should be parsed.
   * @return the parsed method descriptor or null if the given descriptor is invalíd.
   * @throws NullPointerException if the given descriptor is null.
   */
  private static @Nullable TypeDescriptor parseTypeDescriptor(@NonNull String descriptor) {
    try {
      return MethodTypeDesc.ofDescriptor(descriptor);
    } catch (IllegalArgumentException _) {
      return null;
    }
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
  public @NonNull RPCInvocationResult handle(@NonNull RPCInvocationContext context) {
    // get the instance we're working with
    var contextualInstance = context.workingInstance();
    var workingInstance = contextualInstance != null ? contextualInstance : this.boundInstance;
    if (workingInstance == null) {
      return new RPCInvocationResult.ServerError("no instance to invoke the method on", this);
    }

    // parse the method type to validate it
    var targetMethodType = parseTypeDescriptor(context.methodDescriptor());
    if (targetMethodType == null) {
      return new RPCInvocationResult.BadRequest("invalid target method descriptor", this);
    }

    // find the associated method meta in the target class
    var targetMethod = this.targetClassMeta.findMethod(context.methodName(), targetMethodType);
    if (targetMethod == null) {
      return new RPCInvocationResult.BadRequest("target method not found", this);
    }

    // deserialize the provided method arguments, returns null in case the arguments buffer
    // has some invalid argument data at some position
    var methodArguments = this.deserializeMethodArguments(targetMethod, context.argumentInformation());
    if (methodArguments == null) {
      var targetDescriptor = targetMethod.methodType().descriptorString();
      var msg = String.format("provided arguments do not satisfy %s", targetDescriptor);
      return new RPCInvocationResult.BadRequest(msg, this);
    }

    // get or create a method invoker for the target method
    var maybeMethodInvoker = this.getOrCreateMethodInvoker(targetMethod);
    if (maybeMethodInvoker.isFailure()) {
      var constructionException = maybeMethodInvoker.getCause();
      LOGGER.severe("unable to create method invoker for %s", constructionException, targetMethod);
      return new RPCInvocationResult.ServerError("unable to create method invoker", this);
    }

    try {
      var methodInvoker = maybeMethodInvoker.get();
      var invocationResult = methodInvoker.callMethod(workingInstance, methodArguments);
      return new RPCInvocationResult.Success(invocationResult, this, targetMethod);
    } catch (Throwable throwable) {
      return new RPCInvocationResult.Failure(throwable, this, targetMethod);
    }
  }

  /**
   * Gets an existing method invoker for the given target method from the cache or creates a new one. A failure is
   * returned in case a method invoker couldn't be generated for some reason.
   *
   * @param methodMetadata the metadata of the method to get or create the method invoker for.
   * @return a method invoker for the given target method.
   * @throws NullPointerException if the given method metadata is null.
   */
  protected @NonNull Try<MethodInvoker> getOrCreateMethodInvoker(@NonNull RPCMethodMetadata methodMetadata) {
    return Try.of(() -> this.methodInvokerCache.get(methodMetadata, MethodInvokerGenerator::makeMethodInvoker));
  }

  /**
   * Deserializes the method arguments provided in the given buffer based on the generic parameter types provided from
   * the given target method metadata (in order). If an argument at a position is invalid (e.g. bad data or null for a
   * primitive) this method returns null.
   *
   * @param targetMethod           the target method to deserialize the method arguments for.
   * @param encodedArgumentsBuffer the buffer which holds the encoded method arguments to deserialize.
   * @return an array containing the deserialized method arguments, null if the buffer contained bad argument data.
   * @throws NullPointerException if the given target method or argument buffer is null.
   */
  protected @Nullable Object[] deserializeMethodArguments(
    @NonNull RPCMethodMetadata targetMethod,
    @NonNull DataBuf encodedArgumentsBuffer
  ) {
    try {
      var paramTypes = targetMethod.parameterTypes();
      var methodArguments = new Object[paramTypes.length];
      for (var index = 0; index < paramTypes.length; index++) {
        var parameterType = paramTypes[index];
        var parameterValue = this.objectMapper.readObject(encodedArgumentsBuffer, parameterType);
        if (parameterType instanceof Class<?> clazz && clazz.isPrimitive() && parameterValue == null) {
          // primitive value can't be null, method invocation will fail
          return null;
        }

        methodArguments[index] = parameterValue;
      }

      return methodArguments;
    } catch (Exception _) {
      return null;
    }
  }
}
