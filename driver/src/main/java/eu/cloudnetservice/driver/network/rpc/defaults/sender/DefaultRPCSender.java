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

package eu.cloudnetservice.driver.network.rpc.defaults.sender;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.defaults.rpc.DefaultRPC;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * The default implementation of a rpc sender.
 *
 * @since 4.0
 */
public class DefaultRPCSender extends DefaultRPCProvider implements RPCSender {

  protected static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  protected final RPCClassMetadata rpcTargetMeta;
  protected final Supplier<NetworkChannel> channelSupplier;

  /**
   * Constructs a new default rpc provider instance.
   *
   * @param targetClass    the target class of method calls handled by this provider.
   * @param objectMapper   the object mapper to use to write and read data from the buffers.
   * @param dataBufFactory the buffer factory used for buffer allocations.
   * @throws NullPointerException if the given class, object mapper or data buf factory is null.
   */
  protected DefaultRPCSender(
    @NonNull Class<?> targetClass,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory,
    @NonNull RPCClassMetadata rpcTargetMeta,
    @NonNull Supplier<NetworkChannel> channelSupplier
  ) {
    super(targetClass, objectMapper, dataBufFactory);
    this.rpcTargetMeta = rpcTargetMeta;
    this.channelSupplier = channelSupplier;
  }

  @Override
  public @NonNull RPC invokeCaller(Object... args) {
    // offset must be 1 to skip this method
    return this.invokeCaller(1, args);
  }

  @Override
  public @NonNull RPC invokeCaller(int callerStackOffset, Object... args) {
    // offset + 1 to skip this method
    var callerFrame = STACK_WALKER
      .walk(frameStream -> frameStream.skip(callerStackOffset + 1).findFirst())
      .orElseThrow(() -> new IllegalStateException("unable to resolve caller of method"));
    return this.invokeMethod(callerFrame.getMethodName(), callerFrame.getMethodType(), args);
  }

  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName, Object... args) {
    // try to find a unique method with the given name and param count
    var matchingMethods = this.rpcTargetMeta.findMethods(meta -> {
      var methodType = meta.methodType();
      return meta.name().equals(methodName) && methodType.parameterCount() == args.length;
    });
    if (matchingMethods.size() != 1) {
      throw new IllegalArgumentException("Cannot find distinct method to call in "
        + this.targetClass.getSimpleName()
        + " " + methodName + " (" + args.length + " parameters)");
    }

    return this.invokeMethod(matchingMethods.getFirst(), args);
  }

  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName, @NonNull MethodType methodDesc, Object... args) {
    // try to find a valid method meta based on the given name & method descriptor
    var methodMeta = this.rpcTargetMeta.findMethod(methodName, methodDesc);
    if (methodMeta == null) {
      throw new IllegalArgumentException(
        "Cannot find method " + methodName + methodDesc + " in " + this.targetClass.getSimpleName());
    }

    return this.invokeMethod(methodMeta, args);
  }

  private @NonNull RPC invokeMethod(@NonNull RPCMethodMetadata method, Object... args) {
    if (method.ignored()) {
      // method was explicitly ignored, deny calling it
      throw new IllegalArgumentException(
        "Cannot invoke method method " + method.descriptorString() + " as it was explicitly ignored");
    }

    var expectedArgCount = method.methodType().parameterCount();
    if (expectedArgCount != args.length) {
      // argument count for invocation does not match
      throw new IllegalArgumentException(
        "Invalid argument count passed for method invocation. Expected " + expectedArgCount + ", got " + args.length);
    }

    // resolve execution timeout, if not present no timeout is applied
    var methodExecutionTimeout = method.executionTimeout();
    var timeout = methodExecutionTimeout != null ? methodExecutionTimeout : this.rpcTargetMeta.defaultRPCTimeout();

    return new DefaultRPC(
      this.targetClass,
      this.objectMapper,
      this.dataBufFactory,
      this,
      this.channelSupplier,
      timeout,
      method,
      args);
  }
}
