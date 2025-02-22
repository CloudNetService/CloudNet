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

package eu.cloudnetservice.driver.impl.network.rpc.sender;

import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCProvider;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.DefaultRPCMethodMetadata;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.impl.network.rpc.rpc.DefaultRPC;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import java.lang.invoke.TypeDescriptor;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * The default implementation of an RPC sender.
 *
 * @since 4.0
 */
public final class DefaultRPCSender extends DefaultRPCProvider implements RPCSender {

  private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private final RPCClassMetadata rpcTargetMeta;
  private final Supplier<NetworkChannel> channelSupplier;

  /**
   * Constructs a new default rpc sender instance.
   *
   * @param targetClass     the class in which this sender can call methods.
   * @param sourceFactory   the rpc factory that constructed this object.
   * @param objectMapper    the object mapper to use to write and read data from the buffers.
   * @param dataBufFactory  the buffer factory used for buffer allocations.
   * @param rpcTargetMeta   the metadata of the target class handled by this sender.
   * @param channelSupplier the channel supplier for RPCs without a specified target channel.
   * @throws NullPointerException if one of the given parameters is null.
   */
  public DefaultRPCSender(
    @NonNull Class<?> targetClass,
    @NonNull RPCFactory sourceFactory,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory,
    @NonNull RPCClassMetadata rpcTargetMeta,
    @NonNull Supplier<NetworkChannel> channelSupplier
  ) {
    super(targetClass, sourceFactory, objectMapper, dataBufFactory);
    this.rpcTargetMeta = rpcTargetMeta;
    this.channelSupplier = channelSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC invokeCaller(Object... args) {
    // offset must be 1 to skip this method
    return this.invokeCallerWithOffset(1, args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC invokeCallerWithOffset(int callerStackOffset, Object... args) {
    // offset + 1 to skip this method
    var callerFrame = STACK_WALKER
      .walk(frameStream -> frameStream.skip(callerStackOffset + 1).findFirst())
      .orElseThrow(() -> new IllegalStateException("unable to resolve caller of method"));
    return this.invokeMethod(callerFrame.getMethodName(), callerFrame.getMethodType(), args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName, Object... args) {
    // try to find a unique method with the given name and param count
    var matchingMethods = this.rpcTargetMeta.findMethods(meta -> {
      var methodType = meta.methodType();
      return meta.name().equals(methodName) && methodType.parameterCount() == args.length;
    });
    if (matchingMethods.size() != 1) {
      throw new IllegalArgumentException(String.format(
        "Cannot find distinct method to call in %s [searched for %s(%d params)], found %d matches",
        this.targetClass.getName(), methodName, args.length, matchingMethods.size()));
    }

    return this.invokeMethod(matchingMethods.getFirst(), args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName, @NonNull TypeDescriptor methodDesc, Object... args) {
    // try to find a valid method meta based on the given name & method descriptor
    var methodMeta = this.rpcTargetMeta.findMethod(methodName, methodDesc);
    if (methodMeta == null) {
      throw new IllegalArgumentException(String.format(
        "Cannot find a method matching %s%s in %s",
        methodName, methodDesc.descriptorString(), this.targetClass.getName()));
    }

    return this.invokeMethod(methodMeta, args);
  }

  /**
   * Creates a new RPC to call the given target method with the given arguments.
   *
   * @param method the metadata of the method that should be called.
   * @param args   the arguments of to supply to the target method.
   * @return a new RPC instance targeting the given methods with the given arguments.
   * @throws NullPointerException     if the given method metadata or argument array is null.
   * @throws IllegalArgumentException if the parameter count mismatches.
   */
  private @NonNull RPC invokeMethod(@NonNull DefaultRPCMethodMetadata method, Object... args) {
    var expectedArgCount = method.methodType().parameterCount();
    if (expectedArgCount != args.length) {
      // argument count for invocation does not match
      throw new IllegalArgumentException(String.format(
        "Invalid argument count passed for method invocation. Expected %d, got %d",
        expectedArgCount, args.length));
    }

    // resolve execution timeout, if not present no timeout is applied
    var methodExecutionTimeout = method.executionTimeout();
    var timeout = methodExecutionTimeout != null ? methodExecutionTimeout : this.rpcTargetMeta.defaultRPCTimeout();

    return new DefaultRPC(
      this.targetClass,
      this.sourceFactory,
      this.objectMapper,
      this.dataBufFactory,
      this,
      this.channelSupplier,
      timeout,
      method,
      args);
  }
}
