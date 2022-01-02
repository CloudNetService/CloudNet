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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler.context;

import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCInvocationContext;
import java.util.Optional;
import lombok.NonNull;

public class DefaultRPCInvocationContext implements RPCInvocationContext {

  protected int argumentCount;

  protected boolean expectsMethodResult;
  protected boolean normalizePrimitives;
  protected boolean strictInstanceUsage;

  protected String methodName;
  protected NetworkChannel channel;

  protected DataBuf arguments;
  protected Object workingInstance;

  public DefaultRPCInvocationContext(
    boolean expectsMethodResult,
    boolean normalizePrimitives,
    String methodName,
    NetworkChannel channel,
    DataBuf arguments,
    Object workingInstance
  ) {
    this.expectsMethodResult = expectsMethodResult;
    this.normalizePrimitives = normalizePrimitives;
    this.methodName = methodName;
    this.channel = channel;
    this.arguments = arguments;
    this.workingInstance = workingInstance;
  }

  protected DefaultRPCInvocationContext() {
  }

  @Override
  public int argumentCount() {
    return this.argumentCount;
  }

  @Override
  public boolean expectsMethodResult() {
    return this.expectsMethodResult;
  }

  @Override
  public boolean normalizePrimitives() {
    return this.normalizePrimitives;
  }

  @Override
  public boolean strictInstanceUsage() {
    return this.strictInstanceUsage;
  }

  @Override
  public @NonNull String methodName() {
    return this.methodName;
  }

  @Override
  public @NonNull NetworkChannel channel() {
    return this.channel;
  }

  @Override
  public @NonNull DataBuf argumentInformation() {
    return this.arguments;
  }

  @Override
  public @NonNull Optional<Object> workingInstance() {
    return Optional.ofNullable(this.workingInstance);
  }
}
