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
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.exception.RPCException;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.packet.RPCRequestPacket;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a rpc.
 *
 * @since 4.0
 */
public class DefaultRPC extends DefaultRPCProvider implements RPC {

  private final RPCSender sender;
  private final String className;
  private final String methodName;
  private final Object[] arguments;
  private final Type expectedResultType;

  private boolean resultExpectation = true;

  /**
   * Constructs a new default rpc instance.
   *
   * @param sender             the sender of this rpc.
   * @param clazz              the target class of this rpc.
   * @param methodName         the name of the method which should get invoked.
   * @param arguments          the arguments which should get supplied to the method to invoke.
   * @param objectMapper       the object mapper used to write/read data from constructed buffers.
   * @param expectedResultType true if this rpc execution expects a result, false otherwise.
   * @param dataBufFactory     the data buf factory to use for data buf allocation.
   * @throws NullPointerException if one of the given arguments is null.
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCChain join(@NonNull RPC rpc) {
    return new DefaultRPCChain(this, rpc);
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
    return this.className;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String methodName() {
    return this.methodName;
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
    return this.expectedResultType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC disableResultExpectation() {
    this.resultExpectation = false;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean expectsResult() {
    return this.resultExpectation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget() {
    this.fireAndForget(Objects.requireNonNull(this.sender.associatedComponent().firstChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @Nullable T fireSync() {
    return this.fireSync(Objects.requireNonNull(this.sender.associatedComponent().firstChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> Task<T> fire() {
    return this.fire(Objects.requireNonNull(this.sender.associatedComponent().firstChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireAndForget(@NonNull NetworkChannel component) {
    this.disableResultExpectation().fireSync(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @Nullable T fireSync(@NonNull NetworkChannel component) {
    try {
      Task<T> queryTask = this.fire(component);
      return queryTask.get();
    } catch (ExecutionException exception) {
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
      return Task.wrapFuture(component
        .sendQueryAsync(new RPCRequestPacket(dataBuf))
        .thenApply(new RPCResultMapper<>(this.expectedResultType, this.objectMapper)));
    } else {
      // just send the method invocation request
      component.sendPacket(new RPCRequestPacket(dataBuf));
      return Task.completedTask(null);
    }
  }
}
