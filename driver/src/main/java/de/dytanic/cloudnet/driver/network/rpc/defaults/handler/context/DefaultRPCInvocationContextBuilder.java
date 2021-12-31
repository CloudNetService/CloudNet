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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler.context;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.RPCInvocationContext;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRPCInvocationContextBuilder implements RPCInvocationContext.Builder {

  protected final DefaultRPCInvocationContext context = new DefaultRPCInvocationContext();

  @Override
  public @NonNull RPCInvocationContext.Builder argumentCount(int argumentCount) {
    context.argumentCount = argumentCount;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder expectsMethodResult(boolean expectsResult) {
    this.context.expectsMethodResult = expectsResult;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder normalizePrimitives(boolean normalizePrimitives) {
    this.context.normalizePrimitives = normalizePrimitives;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder strictInstanceUsage(boolean strictInstanceUsage) {
    this.context.strictInstanceUsage = strictInstanceUsage;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder methodName(@NonNull String methodName) {
    this.context.methodName = methodName;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder channel(@NonNull NetworkChannel channel) {
    this.context.channel = channel;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder argumentInformation(@NonNull DataBuf information) {
    this.context.arguments = information;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext.Builder workingInstance(@Nullable Object instance) {
    this.context.workingInstance = instance;
    return this;
  }

  @Override
  public @NonNull RPCInvocationContext build() {
    // validate the context
    Verify.verifyNotNull(this.context.arguments, "No arguments supplied");
    Verify.verifyNotNull(this.context.channel, "No source channel supplied");
    Verify.verifyNotNull(this.context.methodName, "No method name supplied");
    // return the created context
    return this.context;
  }
}
