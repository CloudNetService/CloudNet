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

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
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
    // check if the invocation is chained
    if (packet.getContent().readBoolean()) {
      // get the chain size
      int chainSize = packet.getContent().readInt();
      // invoke the method on the current result
      Object lastResult = null;
      for (int i = 1; i < chainSize; i++) {
        if (i == 1) {
          // always invoke the first method
          lastResult = this.handleRPC(channel, packet, null);
        } else if (lastResult != null) {
          // only invoke upcoming methods if there was a previous result
          lastResult = this.handleRPC(channel, packet, lastResult);
        } else {
          // just process over to remove the content from the buffer
          this.handleRPC(channel, packet, null);
        }
      }
      // the last handler decides over the method invocation result
      result = this.handle(channel, packet, lastResult);
    } else {
      // just invoke the method
      result = this.handle(channel, packet, null);
    }
    // check if we need to send a result
    if (result != null && packet.getUniqueId() != null) {
      channel.getQueryPacketManager().sendQueryPacket(new Packet(-1, result), packet.getUniqueId());
    }
  }

  protected @Nullable DataBuf handle(@NotNull INetworkChannel channel, @NotNull IPacket packet, @Nullable Object on) {
    // get the handler associated with the class of the rpc
    RPCHandler handler = this.rpcHandlerRegistry.getHandler(packet.getContent().readString());
    // check if the method gets called on a specific instance
    if (on != null) {
      // invoke the handler with the information
      return handler == null ? null : handler.handleOn(on, channel, packet.getContent());
    } else {
      // invoke the handler with the information
      return handler == null ? null : handler.handleRPC(channel, packet.getContent());
    }
  }

  protected @Nullable Object handleRPC(@NotNull INetworkChannel channel, @NotNull IPacket packet, @Nullable Object on) {
    // get the handler associated with the class of the rpc
    RPCHandler handler = this.rpcHandlerRegistry.getHandler(packet.getContent().readString());
    // check if the method gets called on a specific instance
    if (on != null) {
      // invoke the handler with the information
      return handler == null ? null : handler.handleRawOn(on, channel, packet.getContent());
    } else {
      // invoke the handler with the information
      return handler == null ? null : handler.handleRaw(channel, packet.getContent());
    }
  }
}
