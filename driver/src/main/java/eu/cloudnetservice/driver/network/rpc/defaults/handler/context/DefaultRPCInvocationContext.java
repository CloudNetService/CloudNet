/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.defaults.handler.context;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.RPCInvocationContext;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a rpc invocation context.
 *
 * @since 4.0
 */
public class DefaultRPCInvocationContext implements RPCInvocationContext {

  protected final int argumentCount;

  protected final boolean expectsMethodResult;
  protected final boolean normalizePrimitives;
  protected final boolean strictInstanceUsage;

  protected final String methodName;
  protected final NetworkChannel channel;

  protected final DataBuf arguments;
  protected final Object workingInstance;

  /**
   * Constructs a new default rpc invocation context instance.
   *
   * @param argumentCount       the number of arguments the target method has.
   * @param expectsMethodResult if true the caller of the method expects a result being sent back to the sender.
   * @param normalizePrimitives if true primitive data types which are null will be normalized to their default value.
   * @param strictInstanceUsage if true the caller will not use any other instance than provided to this context.
   * @param methodName          the name of the method to invoke.
   * @param channel             the channel from which the invocation request came.
   * @param arguments           the buffer containing the arguments for the method invocation.
   * @param workingInstance     the instance to work with, null if no binding instance should be used.
   * @throws NullPointerException if either the given method name, channel or argument buffer is null.
   */
  protected DefaultRPCInvocationContext(
    int argumentCount,
    boolean expectsMethodResult,
    boolean normalizePrimitives,
    boolean strictInstanceUsage,
    @NonNull String methodName,
    @NonNull NetworkChannel channel,
    @NonNull DataBuf arguments,
    @Nullable Object workingInstance
  ) {
    this.argumentCount = argumentCount;
    this.expectsMethodResult = expectsMethodResult;
    this.normalizePrimitives = normalizePrimitives;
    this.strictInstanceUsage = strictInstanceUsage;
    this.methodName = methodName;
    this.channel = channel;
    this.arguments = arguments;
    this.workingInstance = workingInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int argumentCount() {
    return this.argumentCount;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean expectsMethodResult() {
    return this.expectsMethodResult;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean normalizePrimitives() {
    return this.normalizePrimitives;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean strictInstanceUsage() {
    return this.strictInstanceUsage;
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
  public @NonNull NetworkChannel channel() {
    return this.channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf argumentInformation() {
    return this.arguments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object workingInstance() {
    return this.workingInstance;
  }
}
