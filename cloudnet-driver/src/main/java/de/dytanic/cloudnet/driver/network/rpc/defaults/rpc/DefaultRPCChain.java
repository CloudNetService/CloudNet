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

package de.dytanic.cloudnet.driver.network.rpc.defaults.rpc;

import com.google.common.collect.Lists;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCChain;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProvider;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.util.ExceptionalResultUtils;
import de.dytanic.cloudnet.driver.network.rpc.exception.RPCException;
import de.dytanic.cloudnet.driver.network.rpc.exception.RPCExecutionException;
import de.dytanic.cloudnet.driver.network.rpc.packet.RPCQueryPacket;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;

public class DefaultRPCChain extends DefaultRPCProvider implements RPCChain {

  protected final RPC rootRPC;
  protected final RPC headRPC;
  protected final List<RPC> rpcChain;

  protected DefaultRPCChain(
    @NotNull RPC rootRPC,
    @NotNull RPC headRPC
  ) {
    this(rootRPC, headRPC, Lists.newArrayList(headRPC));
  }

  protected DefaultRPCChain(
    @NotNull RPC rootRPC,
    @NotNull RPC headRPC,
    @NotNull List<RPC> rpcChain
  ) {
    super(headRPC.getTargetClass(), headRPC.getObjectMapper(), headRPC.getDataBufFactory());

    this.rootRPC = rootRPC;
    this.headRPC = headRPC;
    this.rpcChain = rpcChain;
  }

  @Override
  public @NotNull RPCChain join(@NotNull RPC rpc) {
    // add the rpc call to chain
    this.rpcChain.add(rpc);
    // create the new chain instance
    return new DefaultRPCChain(this.rootRPC, rpc, this.rpcChain);
  }

  @Override
  public @NotNull RPC getRootRPC() {
    return this.rootRPC;
  }

  @Override
  public @NotNull Collection<RPC> getJoins() {
    return Collections.unmodifiableCollection(this.rpcChain);
  }

  @Override
  public void fireAndForget() {
    this.fireAndForget(Objects.requireNonNull(this.rootRPC.getSender().getAssociatedComponent().getFirstChannel()));
  }

  @Override
  public <T> @NotNull T fireSync() {
    return this.fireSync(Objects.requireNonNull(this.rootRPC.getSender().getAssociatedComponent().getFirstChannel()));
  }

  @Override
  public @NotNull <T> ITask<T> fire() {
    return this.fire(Objects.requireNonNull(this.rootRPC.getSender().getAssociatedComponent().getFirstChannel()));
  }

  @Override
  public void fireAndForget(@NotNull INetworkChannel component) {
    this.headRPC.disableResultExpectation();
    this.fireSync(component);
  }

  @Override
  public <T> @NotNull T fireSync(@NotNull INetworkChannel component) {
    try {
      ITask<T> queryTask = this.fire(component);
      return queryTask.get();
    } catch (InterruptedException | ExecutionException exception) {
      if (exception.getCause() instanceof RPCExecutionException) {
        // may be thrown when the handler did throw an exception, just rethrow that one
        throw (RPCExecutionException) exception.getCause();
      } else {
        // any other exception should get wrapped
        throw new RPCException(this, exception);
      }
    }
  }

  @Override
  public @NotNull <T> ITask<T> fire(@NotNull INetworkChannel component) {
    // information about the root invocation
    DataBuf.Mutable dataBuf = this.dataBufFactory.createEmpty()
      .writeBoolean(true) // method chain
      .writeInt(this.rpcChain.size() + 1); // chain length (+1 because the root chain is not included)
    // write the root rpc first
    this.writeRPCInformation(dataBuf, this.rootRPC, false); // the root rpc can never the last
    // write the full chain
    for (int i = 0; i < this.rpcChain.size(); i++) {
      this.writeRPCInformation(dataBuf, this.rpcChain.get(i), i < (this.rpcChain.size() - 1));
    }
    // send query if result is needed
    if (this.headRPC.expectsResult()) {
      // now send the query and read the response
      return component.sendQueryAsync(new RPCQueryPacket(dataBuf))
        .map(IPacket::getContent)
        .map(content -> {
          if (content.readBoolean()) {
            // the execution did not throw an exception
            return this.objectMapper.readObject(content, this.headRPC.getExpectedResultType());
          } else {
            // rethrow the execution exception
            ExceptionalResultUtils.rethrowException(content);
            return null; // ok fine, but this will never happen - no one was seen again after entering the rethrowException method
          }
        });
    } else {
      // just send the method invocation request
      component.sendPacket(new RPCQueryPacket(dataBuf));
      return CompletedTask.emptyTask();
    }
  }

  protected void writeRPCInformation(@NotNull DataBuf.Mutable dataBuf, @NotNull RPC rpc, boolean last) {
    // general information about the rpc invocation
    dataBuf
      .writeString(rpc.getClassName())
      .writeString(rpc.getMethodName())
      .writeBoolean(!last || rpc.expectsResult());
    // write the arguments provided
    for (Object argument : rpc.getArguments()) {
      this.objectMapper.writeObject(dataBuf, argument);
    }
  }
}
