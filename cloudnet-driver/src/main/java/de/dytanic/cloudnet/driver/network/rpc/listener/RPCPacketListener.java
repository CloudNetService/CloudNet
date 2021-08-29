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

package de.dytanic.cloudnet.driver.network.rpc.listener;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.RPCInvocationContext;
import de.dytanic.cloudnet.driver.network.rpc.defaults.MethodInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RPCPacketListener implements IPacketListener {

  private final RPCHandlerRegistry rpcHandlerRegistry;

  public RPCPacketListener(RPCHandlerRegistry rpcHandlerRegistry) {
    this.rpcHandlerRegistry = rpcHandlerRegistry;
  }

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    // the result of the invocation, encoded
    DataBuf result;
    // the input information we get
    DataBuf buf = packet.getContent();
    // check if the invocation is chained
    if (buf.readBoolean()) {
      // get the chain size
      int chainSize = buf.readInt();
      // invoke the method on the current result
      Object lastResult = null;
      for (int i = 1; i < chainSize; i++) {
        if (i == 1) {
          // always invoke the first method
          lastResult = this.handleRaw(buf.readString(), this.buildContext(channel, buf, null, false));
        } else if (lastResult != null) {
          // only invoke upcoming methods if there was a previous result
          lastResult = this.handleRaw(buf.readString(), this.buildContext(channel, buf, lastResult, true));
        } else {
          // just process over to remove the content from the buffer
          this.handleRaw(buf.readString(), this.buildContext(channel, buf, null, true));
        }
      }
      // the last handler decides over the method invocation result
      result = this.handle(buf.readString(), this.buildContext(channel, buf, lastResult, true));
    } else {
      // just invoke the method
      result = this.handle(buf.readString(), this.buildContext(channel, buf, null, false));
    }
    // check if we need to send a result
    if (result != null && packet.getUniqueId() != null) {
      channel.getQueryPacketManager().sendQueryPacket(new Packet(-1, result), packet.getUniqueId());
    }
  }

  protected @Nullable DataBuf handle(@NotNull String clazz, @NotNull RPCInvocationContext context) {
    // get the handler associated with the class of the rpc
    RPCHandler handler = this.rpcHandlerRegistry.getHandler(clazz);
    // check if the method gets called on a specific instance
    if (handler != null) {
      // invoke the method
      Pair<Object, MethodInformation> methodInvocationResult = handler.handle(context);
      // check if the sender expect the result of the method
      if (context.expectsMethodResult()) {
        // check if the method return void
        if (methodInvocationResult.getSecond().isVoidMethod()) {
          return handler.getDataBufFactory().createWithExpectedSize(1).writeBoolean(false);
        } else {
          // write the result of the invocation
          return handler.getObjectMapper().writeObject(
            handler.getDataBufFactory().createEmpty(),
            methodInvocationResult.getFirst());
        }
      }
    }
    // no result expected or no handler
    return null;
  }

  protected @Nullable Object handleRaw(@NotNull String clazz, @NotNull RPCInvocationContext context) {
    // get the handler associated with the class of the rpc
    RPCHandler handler = this.rpcHandlerRegistry.getHandler(clazz);
    // invoke the handler with the information
    return handler == null ? null : handler.handle(context).getFirst();
  }

  protected @NotNull RPCInvocationContext buildContext(
    @NotNull INetworkChannel channel,
    @NotNull DataBuf content,
    @Nullable Object on,
    boolean strictInstanceUsage
  ) {
    return RPCInvocationContext.builder()
      .workingInstance(on)
      .channel(channel)
      .methodName(content.readString())
      .expectsMethodResult(content.readBoolean())
      .argumentInformation(content)
      .normalizePrimitives(Boolean.TRUE)
      .strictInstanceUsage(strictInstanceUsage)
      .build();
  }
}
