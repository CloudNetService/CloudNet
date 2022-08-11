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

package eu.cloudnetservice.driver.network.rpc.defaults.rpc;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.exception.RPCException;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import eu.cloudnetservice.driver.network.rpc.packet.RPCRequestPacket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;

/**
 * Represents the default implementation of a rpc chain.
 *
 * @since 4.0
 */
public class DefaultRPCChain extends DefaultRPCProvider implements RPCChain {

  protected final RPC rootRPC;
  protected final RPC headRPC;
  protected final List<RPC> rpcChain;

  /**
   * Constructs a new default rpc chain instance.
   *
   * @param rootRPC the root rpc of the chain, also known as the entry point.
   * @param headRPC the next rpc to invoke after the root rpc.
   * @throws NullPointerException if either the given root or head rpc is null.
   */
  protected DefaultRPCChain(
    @NonNull RPC rootRPC,
    @NonNull RPC headRPC
  ) {
    this(rootRPC, headRPC, List.of(headRPC));
  }

  /**
   * Constructs a new default rpc chain instance.
   *
   * @param rootRPC  the root rpc of the chain, also known as the entry point.
   * @param headRPC  the next rpc to invoke after the root rpc.
   * @param rpcChain the full chain of rpc invocations to call on top of the root rpc.
   * @throws NullPointerException if either the given root or head rpc is null.
   */
  protected DefaultRPCChain(
    @NonNull RPC rootRPC,
    @NonNull RPC headRPC,
    @NonNull List<RPC> rpcChain
  ) {
    super(headRPC.targetClass(), headRPC.objectMapper(), headRPC.dataBufFactory());

    this.rootRPC = rootRPC;
    this.headRPC = headRPC;
    this.rpcChain = new LinkedList<>(rpcChain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCChain join(@NonNull RPC rpc) {
    // add the rpc call to chain
    var newChain = new LinkedList<>(this.rpcChain);
    newChain.add(rpc);
    // create the new chain instance
    return new DefaultRPCChain(this.rootRPC, rpc, newChain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC head() {
    return this.rootRPC;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<RPC> joins() {
    return this.rpcChain;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget() {
    this.fireAndForget(Objects.requireNonNull(this.rootRPC.sender().associatedComponent().firstChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull T fireSync() {
    return this.fireSync(Objects.requireNonNull(this.rootRPC.sender().associatedComponent().firstChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> Task<T> fire() {
    return this.fire(Objects.requireNonNull(this.rootRPC.sender().associatedComponent().firstChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget(@NonNull NetworkChannel component) {
    this.headRPC.disableResultExpectation();
    this.fireSync(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull T fireSync(@NonNull NetworkChannel component) {
    try {
      Task<T> queryTask = this.fire(component);
      return queryTask.get();
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof RPCExecutionException) {
        // may be thrown when the handler did throw an exception, just rethrow that one
        throw (RPCExecutionException) exception.getCause();
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
    // information about the root invocation
    var dataBuf = this.dataBufFactory.createEmpty()
      .writeBoolean(true) // method chain
      .writeInt(this.rpcChain.size() + 1); // chain length (+1 because the root chain is not included)
    // write the root rpc first
    this.writeRPCInformation(dataBuf, this.rootRPC, false); // the root rpc can never the last
    // write the full chain
    for (var i = 0; i < this.rpcChain.size(); i++) {
      this.writeRPCInformation(dataBuf, this.rpcChain.get(i), i < (this.rpcChain.size() - 1));
    }
    // send query if result is needed
    if (this.headRPC.expectsResult()) {
      // now send the query and read the response
      return Task.wrapFuture(component
        .sendQueryAsync(new RPCRequestPacket(dataBuf))
        .thenApply(new RPCResultMapper<>(this.headRPC.expectedResultType(), this.objectMapper)));
    } else {
      // just send the method invocation request
      component.sendPacket(new RPCRequestPacket(dataBuf));
      return Task.completedTask(null);
    }
  }

  /**
   * Writes the given rpc into the given buffer.
   *
   * @param dataBuf the data buffer to write the rpc to.
   * @param rpc     the rpc to serialize.
   * @param last    true if the given rpc is the last rpc in the call chain, false otherwise.
   * @throws NullPointerException if either the given buffer or rpc is null.
   */
  protected void writeRPCInformation(@NonNull DataBuf.Mutable dataBuf, @NonNull RPC rpc, boolean last) {
    // general information about the rpc invocation
    dataBuf
      .writeString(rpc.className())
      .writeString(rpc.methodName())
      .writeBoolean(!last || rpc.expectsResult())
      .writeInt(rpc.arguments().length);
    // write the arguments provided
    for (var argument : rpc.arguments()) {
      this.objectMapper.writeObject(dataBuf, argument);
    }
  }
}
