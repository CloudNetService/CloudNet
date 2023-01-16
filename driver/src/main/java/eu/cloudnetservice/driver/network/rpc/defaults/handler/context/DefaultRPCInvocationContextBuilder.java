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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.RPCInvocationContext;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a rpc invocation context builder.
 *
 * @see RPCInvocationContext#builder()
 * @since 4.0
 */
public class DefaultRPCInvocationContextBuilder implements RPCInvocationContext.Builder {

  protected int argumentCount = 0;

  protected boolean expectsMethodResult = true;
  protected boolean normalizePrimitives = true;
  protected boolean strictInstanceUsage = false;

  protected String methodName;
  protected NetworkChannel channel;

  protected DataBuf arguments;
  protected Object workingInstance;

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder argumentCount(int argumentCount) {
    this.argumentCount = argumentCount;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder expectsMethodResult(boolean expectsResult) {
    this.expectsMethodResult = expectsResult;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder normalizePrimitives(boolean normalizePrimitives) {
    this.normalizePrimitives = normalizePrimitives;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder strictInstanceUsage(boolean strictInstanceUsage) {
    this.strictInstanceUsage = strictInstanceUsage;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder methodName(@NonNull String methodName) {
    this.methodName = methodName;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder channel(@NonNull NetworkChannel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder argumentInformation(@NonNull DataBuf information) {
    this.arguments = information;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext.Builder workingInstance(@Nullable Object instance) {
    this.workingInstance = instance;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCInvocationContext build() {
    // validate the context
    Preconditions.checkNotNull(this.arguments, "No arguments supplied");
    Preconditions.checkNotNull(this.channel, "No source channel supplied");
    Preconditions.checkNotNull(this.methodName, "No method name supplied");
    // create the context
    return new DefaultRPCInvocationContext(
      this.argumentCount,
      this.expectsMethodResult,
      this.normalizePrimitives,
      this.strictInstanceUsage,
      this.methodName,
      this.channel,
      this.arguments,
      this.workingInstance);
  }
}
