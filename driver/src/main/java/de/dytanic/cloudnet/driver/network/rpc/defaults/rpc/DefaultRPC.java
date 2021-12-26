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

import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCChain;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProvider;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.util.ExceptionalResultUtils;
import de.dytanic.cloudnet.driver.network.rpc.exception.RPCException;
import de.dytanic.cloudnet.driver.network.rpc.exception.RPCExecutionException;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.packet.RPCQueryPacket;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPC extends DefaultRPCProvider implements RPC {

  private final RPCSender sender;
  private final String className;
  private final String methodName;
  private final Object[] arguments;
  private final Type expectedResultType;

  private boolean resultExpectation = true;

  public DefaultRPC(
    @NonNull RPCSender sender,
    @NonNull Class<?> clazz,
    @NonNull String methodName,
    @NonNull Object[] arguments,
    @NonNull ObjectMapper objectMapper,
    @NonNull Type expectedResultType,
    @NonNull DataBufFactory dataBufFactory
  ) {
    super(clazz, objectMapper, dataBufFactory);

    this.sender = sender;
    this.className = clazz.getCanonicalName();
    this.methodName = methodName;
    this.arguments = arguments;
    this.expectedResultType = expectedResultType;
  }

  @Override
  public @NonNull RPCChain join(@NonNull RPC rpc) {
    return new DefaultRPCChain(this, rpc);
  }

  @Override
  public @NonNull RPCSender sender() {
    return this.sender;
  }

  @Override
  public @NonNull String className() {
    return this.className;
  }

  @Override
  public @NonNull String methodName() {
    return this.methodName;
  }

  @Override
  public @NonNull Object[] arguments() {
    return this.arguments;
  }

  @Override
  public @NonNull Type expectedResultType() {
    return this.expectedResultType;
  }

  @Override
  public @NonNull RPC disableResultExpectation() {
    this.resultExpectation = false;
    return this;
  }

  @Override
  public boolean expectsResult() {
    return this.resultExpectation;
  }

  @Override
  public void fireAndForget() {
    this.fireAndForget(Objects.requireNonNull(this.sender.associatedComponent().firstChannel()));
  }

  @Override
  public <T> @Nullable T fireSync() {
    return this.fireSync(Objects.requireNonNull(this.sender.associatedComponent().firstChannel()));
  }

  @Override
  public @NonNull <T> Task<T> fire() {
    return this.fire(Objects.requireNonNull(this.sender.associatedComponent().firstChannel()));
  }

  @Override
  public void fireAndForget(@NonNull NetworkChannel component) {
    this.disableResultExpectation().fireSync(component);
  }

  @Override
  public <T> @Nullable T fireSync(@NonNull NetworkChannel component) {
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
    // write the default needed information we need
    var dataBuf = this.dataBufFactory.createEmpty()
      .writeBoolean(false) // not a method chain
      .writeString(this.className)
      .writeString(this.methodName)
      .writeBoolean(this.resultExpectation)
      .writeInt(this.arguments.length);
    // write the arguments provided
    for (var argument : this.arguments) {
      this.objectMapper.writeObject(dataBuf, argument);
    }
    // send query if result is needed
    if (this.resultExpectation) {
      // now send the query and read the response
      return component.sendQueryAsync(new RPCQueryPacket(dataBuf))
        .map(Packet::content)
        .map(content -> {
          if (content.readBoolean()) {
            // the execution did not throw an exception
            return this.objectMapper.readObject(content, this.expectedResultType);
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
}
