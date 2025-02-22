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

package eu.cloudnetservice.driver.impl.network.rpc;

import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.RPCProvider;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import lombok.NonNull;

/**
 * An abstract implementation of a rpc provider for easier implementation in subclasses.
 *
 * @since 4.0
 */
public abstract class DefaultRPCProvider implements RPCProvider {

  protected final Class<?> targetClass;
  protected final RPCFactory sourceFactory;
  protected final ObjectMapper objectMapper;
  protected final DataBufFactory dataBufFactory;

  /**
   * Constructs a new default rpc provider instance.
   *
   * @param targetClass    the target class of method calls handled by this provider.
   * @param sourceFactory  the rpc factory that constructed this object.
   * @param objectMapper   the object mapper to use to write and read data from the buffers.
   * @param dataBufFactory the buffer factory used for buffer allocations.
   * @throws NullPointerException if the given class, object mapper or data buf factory is null.
   */
  protected DefaultRPCProvider(
    @NonNull Class<?> targetClass,
    @NonNull RPCFactory sourceFactory,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    this.targetClass = targetClass;
    this.sourceFactory = sourceFactory;
    this.objectMapper = objectMapper;
    this.dataBufFactory = dataBufFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<?> targetClass() {
    return this.targetClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ObjectMapper objectMapper() {
    return this.objectMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBufFactory dataBufFactory() {
    return this.dataBufFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCFactory sourceFactory() {
    return this.sourceFactory;
  }
}
