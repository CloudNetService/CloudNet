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

package eu.cloudnetservice.driver.network.rpc;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.exception.RPCException;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents any request that can be executed like a rpc providing both sync and async method execution access.
 *
 * @since 4.0
 */
public interface RPCExecutable {

  /**
   * Fires the current rpc into the first channel of the associated network component and doesn't wait for the result of
   * the rpc to become available. This method will not suspend the current thread.
   *
   * @throws NullPointerException if the associated network component has no channels available.
   */
  @NonBlocking
  void fireAndForget();

  /**
   * Fires the current rpc into the first channel of the associated network component and waits for the result to become
   * available, or times out after 30 seconds. This method will suspend the calling thread.
   *
   * @param <T> the expected return type.
   * @return the result of the method invocation, might be null if the remote method returned null.
   * @throws NullPointerException  if the associated network component has no channels available.
   * @throws RPCExecutionException if any exception occurred on the remote component during the rpc processing.
   * @throws RPCException          if any other exception occurs during the rpc processing.
   */
  @Blocking
  @UnknownNullability
  <T> T fireSync();

  /**
   * Fires the current rpc into the first channel of the associated network component and returns a future which will be
   * completed with the return value of the method execution when it's available or completed with a timeout exception
   * when the query packet future times out.
   *
   * @param <T> the expected return type.
   * @return a task completed with the result of the method invocation, null if the remote method returned null.
   * @throws NullPointerException if the associated network component has no channels available.
   */
  @NonNull
  <T> CompletableFuture<T> fire();

  /**
   * Fires the current rpc into the given network channel and doesn't wait for the result of the rpc to become
   * available. This method will not suspend the current thread.
   *
   * @param component the network channel to which the rpc should be sent.
   * @throws NullPointerException if the given network channel is null.
   */
  @NonBlocking
  void fireAndForget(@NonNull NetworkChannel component);

  /**
   * Fires the current rpc into the given network channel and waits for the result to become available, or times out
   * after 30 seconds. This method will suspend the calling thread.
   *
   * @param component the network channel to which the rpc should be sent.
   * @param <T>       the expected return type.
   * @return the result of the method invocation, might be null if the remote method returned null.
   * @throws NullPointerException  if the given network channel is null.
   * @throws RPCExecutionException if any exception occurred on the remote component during the rpc processing.
   * @throws RPCException          if any other exception occurs during the rpc processing.
   */
  @Blocking
  <T> T fireSync(@NonNull NetworkChannel component);

  /**
   * Fires the current rpc into the given network channel and returns a future which will be completed with the return
   * value of the method execution when it's available or completed with a timeout exception when the query packet
   * future times out. Note: if the target method return a task, this method does not return a task wrapping a task, but
   * rather the return value in the task directly.
   *
   * @param component the network channel to which the rpc should be sent.
   * @param <T>       the expected return type.
   * @return a task completed with the result of the method invocation, null if the remote method returned null.
   * @throws NullPointerException if the given network channel is null.
   */
  @NonNull
  <T> CompletableFuture<T> fire(@NonNull NetworkChannel component);
}
