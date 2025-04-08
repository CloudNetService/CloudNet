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

import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * The base class implemented by anything which is related to rpc.
 *
 * @since 4.0
 */
public interface RPCProvider {

  /**
   * Get the class which is targeted by the associated rpc.
   *
   * @return the target class.
   */
  @NonNull
  Class<?> targetClass();

  /**
   * Get the object mapper which is used for (de-) serialization of objects if needed.
   *
   * @return the associated object mapper.
   */
  @NonNull
  ObjectMapper objectMapper();

  /**
   * Get the data buf factory which is used to allocate network data buffers if needed.
   *
   * @return the associated data buf factory.
   */
  @NonNull
  DataBufFactory dataBufFactory();

  /**
   * Get the rpc factory that constructed this rpc provider object.
   *
   * @return the rpc factory that constructed this rpc provider object.
   */
  @NonNull
  RPCFactory sourceFactory();

  /**
   * A base builder that can be used for everything that implements RPC provider.
   *
   * @param <B> the subtype of the builder being used, for chaining.
   * @since 4.0
   */
  interface Builder<B extends Builder<B>> {

    /**
     * Sets the object mapper for serialization of objects during RPC execution related to the final rpc provider.
     *
     * @param objectMapper the object mapper to use.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given mapper is null.
     */
    @NonNull
    @Contract("_ -> this")
    B objectMapper(@NonNull ObjectMapper objectMapper);

    /**
     * Sets the data buf factory to use for everything that is related with data serialization in the final rpc
     * provider.
     *
     * @param dataBufFactory the data buf factory to use.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given factory is null.
     */
    @NonNull
    @Contract("_ -> this")
    B dataBufFactory(@NonNull DataBufFactory dataBufFactory);
  }
}
