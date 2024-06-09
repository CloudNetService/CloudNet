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

package eu.cloudnetservice.driver.network.rpc.defaults.handler;

import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an RPC handler builder.
 *
 * @param <T> the type that the handler is handling.
 * @since 4.0
 */
public final class DefaultRPCHandlerBuilder<T> implements RPCHandler.Builder<T> {

  private final RPCClassMetadata classMetadata;

  private Object boundInstance;
  private ObjectMapper objectMapper;
  private DataBufFactory dataBufFactory;

  /**
   * Constructs a new builder instance for the target class (given by the class metadata) and the default data buf
   * factory / object mapper from the source RPC factory.
   *
   * @param classMetadata  the metadata for the target class.
   * @param dataBufFactory the default data buf factory of the source RPC factory.
   * @param objectMapper   the default object mapper of the source RPC factory.
   * @throws NullPointerException if the given class meta, data buf factory or object mapper is null.
   */
  public DefaultRPCHandlerBuilder(
    @NonNull RPCClassMetadata classMetadata,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    this.classMetadata = classMetadata;
    this.objectMapper = objectMapper;
    this.dataBufFactory = dataBufFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCHandler.Builder<T> targetInstance(@Nullable Object instance) {
    this.boundInstance = instance;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCHandler.Builder<T> objectMapper(@NonNull ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCHandler.Builder<T> dataBufFactory(@NonNull DataBufFactory dataBufFactory) {
    this.dataBufFactory = dataBufFactory;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCHandler build() {
    var classMetadata = this.classMetadata.freeze(); // immutable & copied - changes no longer reflect into it
    return new DefaultRPCHandler(this.objectMapper, this.dataBufFactory, this.boundInstance, classMetadata);
  }
}
