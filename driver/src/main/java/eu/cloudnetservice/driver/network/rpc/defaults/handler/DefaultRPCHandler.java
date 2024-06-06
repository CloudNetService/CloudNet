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
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationContext;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
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

    try {

    } catch (Throwable throwable) {
      // other failure that is not categorized
      return new RPCInvocationResult.Failure(throwable, this, targetMethod);
    }
  }
}
