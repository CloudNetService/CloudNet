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

package eu.cloudnetservice.driver.network.rpc.defaults.rpc;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.exception.RPCException;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.packet.RPCRequestPacket;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The default implementation of a rpc.
 *
 * @since 4.0
 */
public final class DefaultRPC extends DefaultRPCProvider implements RPC {

  private final RPCSender sender;
  private final Supplier<NetworkChannel> channelSupplier;

  private final Object[] arguments;
  private final RPCMethodMetadata targetMethod;

  private boolean dropResult;
  private Duration executionTimeout;

  public DefaultRPC(
    @NonNull Class<?> targetClass,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory,
    @NonNull RPCSender sender,
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @Nullable Duration executionTimeout,
    @NonNull RPCMethodMetadata targetMethod,
    @NonNull Object[] arguments
  ) {
    super(targetClass, objectMapper, dataBufFactory);
    this.sender = sender;
    this.channelSupplier = channelSupplier;

    this.executionTimeout = executionTimeout;
    this.targetMethod = targetMethod;

    this.arguments = arguments;
    this.dropResult = targetMethod.executionResultIgnored();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCChain join(@NonNull RPC rpc) {
    return DefaultRPCChain.of(this, rpc, this.channelSupplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender sender() {
    return this.sender;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String className() {
    return this.targetClass.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String methodName() {
    return this.targetMethod.name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String methodDescriptor() {
    return this.targetMethod.methodType().descriptorString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object[] arguments() {
    return this.arguments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Type expectedResultType() {
    return this.targetMethod.returnType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC timeout(@Nullable Duration timeout) {
    this.executionTimeout = timeout;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Duration timeout() {
    return this.executionTimeout;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC dropResult() {
    this.dropResult = true;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean resultDropped() {
    return this.dropResult;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget() {
    var targetNetworkChannel = this.channelSupplier.get();
    Objects.requireNonNull(targetNetworkChannel, "unable to get target network channel");
    this.fireAndForget(targetNetworkChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T fireSync() {
    var targetNetworkChannel = this.channelSupplier.get();
    Objects.requireNonNull(targetNetworkChannel, "unable to get target network channel");
    return this.fireSync(targetNetworkChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> Task<T> fire() {
    var targetNetworkChannel = this.channelSupplier.get();
    Objects.requireNonNull(targetNetworkChannel, "unable to get target network channel");
    return this.fire(targetNetworkChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget(@NonNull NetworkChannel component) {
    this.dropResult().fireSync(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @Nullable T fireSync(@NonNull NetworkChannel component) {
    try {
      Task<T> queryTask = this.fire(component);
      return queryTask.get();
    } catch (ExecutionException | CancellationException exception) {
      if (exception.getCause() instanceof RPCExecutionException executionException) {
        // may be thrown when the handler did throw an exception, just rethrow that one
        throw executionException;
      } else {
        // any other exception should get wrapped
        throw new RPCException(this, exception);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt(); // reset the interrupted state of the thread
      throw new IllegalThreadStateException();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> Task<T> fire(@NonNull NetworkChannel component) {
    // write the information about the RPC into a buffer
    var dataBuf = this.dataBufFactory.createEmpty()
      .writeInt(1) // single RPC
      .writeString(this.className())
      .writeString(this.methodName())
      .writeString(this.methodDescriptor());
    for (var argument : this.arguments) {
      this.objectMapper.writeObject(dataBuf, argument);
    }

    if (this.dropResult) {
      // no result expected: send the RPC request (not a query) and just return a completed future
      component.sendPacket(new RPCRequestPacket(dataBuf));
      return Task.completedTask(null);
    } else {
      // result is expected: send a query to the target network component and return the future so that
      // the caller can decide how to wait for the result
      CompletableFuture<T> queryFuture = component
        .sendQueryAsync(new RPCRequestPacket(dataBuf))
        .thenApply(new RPCResultMapper<>(this.expectedResultType(), this.objectMapper));
      if (this.executionTimeout != null) {
        // apply the requested timeout
        var timeoutMillis = this.executionTimeout.toMillis();
        queryFuture = queryFuture.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
      }

      return Task.wrapFuture(queryFuture);
    }
  }
}
