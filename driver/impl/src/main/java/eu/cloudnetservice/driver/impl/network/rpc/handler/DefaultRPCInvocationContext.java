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

package eu.cloudnetservice.driver.impl.network.rpc.handler;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationContext;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an RPC invocation context.
 *
 * @since 4.0
 */
public final class DefaultRPCInvocationContext implements RPCInvocationContext {

  private final String methodName;
  private final String methodDescriptor;
  private final DataBuf argumentInformation;
  private final Object workingInstance;

  /**
   * Constructs a new default RPC invocation context instance, only used by the builder.
   *
   * @param methodName          the name of the method to invoke.
   * @param methodDescriptor    the descriptor of the method to invoke.
   * @param argumentInformation the buffer containing the information about the arguments for the target method.
   * @param workingInstance     the instance on which the method should be called, can be null.
   * @throws NullPointerException if the given method name, descriptor or argument info is null.
   */
  private DefaultRPCInvocationContext(
    @NonNull String methodName,
    @NonNull String methodDescriptor,
    @NonNull DataBuf argumentInformation,
    @Nullable Object workingInstance
  ) {
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
    this.argumentInformation = argumentInformation;
    this.workingInstance = workingInstance;
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
  public @NonNull String methodDescriptor() {
    return this.methodDescriptor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf argumentInformation() {
    return this.argumentInformation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object workingInstance() {
    return this.workingInstance;
  }

  /**
   * The default builder implementation for an RPC invocation context.
   *
   * @since 4.0
   */
  public static final class Builder implements RPCInvocationContext.Builder {

    private String methodName;
    private String methodDescriptor;
    private DataBuf argumentInformation;
    private Object workingInstance;

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
    public @NonNull RPCInvocationContext.Builder methodDescriptor(@NonNull String methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RPCInvocationContext.Builder argumentInformation(@NonNull DataBuf information) {
      this.argumentInformation = information;
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
      Preconditions.checkNotNull(this.methodName, "method name must be given");
      Preconditions.checkNotNull(this.methodDescriptor, "method descriptor must be given");
      Preconditions.checkNotNull(this.argumentInformation, "argument info buffer must be given");
      return new DefaultRPCInvocationContext(
        this.methodName,
        this.methodDescriptor,
        this.argumentInformation,
        this.workingInstance);
    }
  }
}
