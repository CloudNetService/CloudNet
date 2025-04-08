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

package eu.cloudnetservice.driver.impl.network.rpc.rpc;

import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCProvider;
import eu.cloudnetservice.driver.impl.network.rpc.RPCRequestPacket;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.exception.RPCException;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents the default implementation of a rpc chain.
 *
 * @since 4.0
 */
public final class DefaultRPCChain extends DefaultRPCProvider implements RPCChain {

  private final RPC chainHead;
  private final RPC chainTail;
  private final List<RPC> fullChain;
  private final Supplier<NetworkChannel> channelSupplier;

  /**
   * Constructs a new rpc chain instance.
   *
   * @param chainHead       the first rpc in the execution chain.
   * @param chainTail       the last rpc in the execution chain.
   * @param fullChain       the full chain of rpc to invoke.
   * @param channelSupplier the target channel supplier to use for method invocation if no channel is provided.
   * @throws NullPointerException if one of the given parameters is null.
   */
  // trusted constructor as changes to the full chain list will reflect into this instance - do not expose
  private DefaultRPCChain(
    @NonNull RPC chainHead,
    @NonNull RPC chainTail,
    @NonNull List<RPC> fullChain,
    @NonNull Supplier<NetworkChannel> channelSupplier
  ) {
    super(chainHead.targetClass(), chainHead.sourceFactory(), chainHead.objectMapper(), chainHead.dataBufFactory());

    this.chainHead = chainHead;
    this.chainTail = chainTail;
    this.fullChain = Collections.unmodifiableList(fullChain);
    this.channelSupplier = channelSupplier;
  }

  /**
   * Constructs a new rpc chain using the given root and tail rpc.
   *
   * @param root            the root rpc for the chain.
   * @param next            the tail rpc for the chain.
   * @param channelSupplier the target channel supplier to use for method invocation if no channel is provided.
   * @return the constructed rpc chain.
   * @throws NullPointerException if the given root rpc, tail rpc or channel supplier is null.
   */
  public static @NonNull DefaultRPCChain of(
    @NonNull RPC root,
    @NonNull RPC next,
    @NonNull Supplier<NetworkChannel> channelSupplier
  ) {
    List<RPC> fullChain = new LinkedList<>();
    fullChain.add(root);
    fullChain.add(next);
    return new DefaultRPCChain(root, next, fullChain, channelSupplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCChain join(@NonNull RPC rpc) {
    var newChain = new LinkedList<>(this.fullChain);
    newChain.add(rpc);
    return new DefaultRPCChain(this.chainHead, rpc, newChain, this.channelSupplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC tail() {
    return this.chainTail;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<RPC> joins() {
    return this.fullChain;
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
  public @NonNull <T> CompletableFuture<T> fire() {
    var targetNetworkChannel = this.channelSupplier.get();
    Objects.requireNonNull(targetNetworkChannel, "unable to get target network channel");
    return this.fire(targetNetworkChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget(@NonNull NetworkChannel component) {
    this.chainTail.dropResult();
    this.fireSync(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull T fireSync(@NonNull NetworkChannel component) {
    try {
      CompletableFuture<T> queryTask = this.fire(component);
      var invocationResult = queryTask.get();
      if (this.chainTail.targetMethod().asyncReturnType()) {
        // for async methods the fire method does not return the result wrapped in a Future, it returns the raw
        // result. therefore for sync invocation we need re-wrap the result into a future as it is the expected type
        //noinspection unchecked
        return (T) TaskUtil.finishedFuture(invocationResult);
      } else {
        return invocationResult;
      }
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof RPCExecutionException rpcExecutionException) {
        // may be thrown when the handler did throw an exception, just rethrow that one
        throw rpcExecutionException;
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
  public @NonNull <T> CompletableFuture<T> fire(@NonNull NetworkChannel component) {
    // write the chained RPC information
    var buffer = this.dataBufFactory.createEmpty().writeInt(this.fullChain.size());
    for (var chainEntry : this.fullChain) {
      buffer
        .writeString(chainEntry.className())
        .writeString(chainEntry.methodName())
        .writeString(chainEntry.methodDescriptor());
      for (var argument : chainEntry.arguments()) {
        this.objectMapper.writeObject(buffer, argument);
      }
    }

    if (this.chainTail.resultDropped()) {
      // no result expected: send the RPC request (not a query) and just return a completed future after
      component.sendPacket(new RPCRequestPacket(buffer));
      return TaskUtil.finishedFuture(null);
    } else {
      // result is expected: send a query to the target network component and return the future so that
      // the caller can decide how to wait for the result
      CompletableFuture<T> queryFuture = component
        .sendQueryAsync(new RPCRequestPacket(buffer))
        .thenApply(new RPCResultMapper<>(this.chainTail.expectedResultType(), this.objectMapper));

      var timeout = this.chainTail.timeout();
      if (timeout != null) {
        // apply the requested timeout
        var timeoutMillis = timeout.toMillis();
        queryFuture = queryFuture.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
      }

      return queryFuture;
    }
  }
}
