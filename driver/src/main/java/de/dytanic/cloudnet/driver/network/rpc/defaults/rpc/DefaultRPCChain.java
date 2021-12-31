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

package de.dytanic.cloudnet.driver.network.rpc.defaults.rpc;

import com.google.common.collect.Lists;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
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
import lombok.NonNull;

public class DefaultRPCChain extends DefaultRPCProvider implements RPCChain {

  protected final RPC rootRPC;
  protected final RPC headRPC;
  protected final List<RPC> rpcChain;

  protected DefaultRPCChain(
    @NonNull RPC rootRPC,
    @NonNull RPC headRPC
  ) {
    this(rootRPC, headRPC, Lists.newArrayList(headRPC));
  }

  protected DefaultRPCChain(
    @NonNull RPC rootRPC,
    @NonNull RPC headRPC,
    @NonNull List<RPC> rpcChain
  ) {
    super(headRPC.targetClass(), headRPC.objectMapper(), headRPC.dataBufFactory());

    this.rootRPC = rootRPC;
    this.headRPC = headRPC;
    this.rpcChain = rpcChain;
  }

  @Override
  public @NonNull RPCChain join(@NonNull RPC rpc) {
    // add the rpc call to chain
    this.rpcChain.add(rpc);
    // create the new chain instance
    return new DefaultRPCChain(this.rootRPC, rpc, this.rpcChain);
  }

  @Override
  public @NonNull RPC head() {
    return this.rootRPC;
  }

  @Override
  public @NonNull Collection<RPC> joins() {
    return Collections.unmodifiableCollection(this.rpcChain);
  }

  @Override
  public void fireAndForget() {
    this.fireAndForget(Objects.requireNonNull(this.rootRPC.sender().associatedComponent().firstChannel()));
  }

  @Override
  public <T> @NonNull T fireSync() {
    return this.fireSync(Objects.requireNonNull(this.rootRPC.sender().associatedComponent().firstChannel()));
  }

  @Override
  public @NonNull <T> Task<T> fire() {
    return this.fire(Objects.requireNonNull(this.rootRPC.sender().associatedComponent().firstChannel()));
  }

  @Override
  public void fireAndForget(@NonNull NetworkChannel component) {
    this.headRPC.disableResultExpectation();
    this.fireSync(component);
  }

  @Override
  public <T> @NonNull T fireSync(@NonNull NetworkChannel component) {
    try {
      Task<T> queryTask = this.fire(component);
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
      return component.sendQueryAsync(new RPCQueryPacket(dataBuf))
        .map(Packet::content)
        .map(content -> {
          if (content.readBoolean()) {
            // the execution did not throw an exception
            return this.objectMapper.readObject(content, this.headRPC.expectedResultType());
          } else {
            // rethrow the execution exception
            ExceptionalResultUtils.rethrowException(content);
            return null; // ok fine, but this will never happen - no one was seen again after entering the rethrowException method
          }
        });
    } else {
      // just send the method invocation request
      component.sendPacket(new RPCQueryPacket(dataBuf));
      return CompletedTask.done(null);
    }
  }

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
