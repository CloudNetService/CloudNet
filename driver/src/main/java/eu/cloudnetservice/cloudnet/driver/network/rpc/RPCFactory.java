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

package eu.cloudnetservice.cloudnet.driver.network.rpc;

import eu.cloudnetservice.cloudnet.driver.network.NetworkComponent;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A factory which can provide anything which is related to rpc.
 *
 * @since 4.0
 */
public interface RPCFactory {

  /**
   * Get the default object mapper used by this factory if no other mapper is supplied to a factory method.
   *
   * @return the default object mapper of this factory.
   */
  @NonNull ObjectMapper defaultObjectMapper();

  /**
   * Get the default data buf factory used by this factory if no other data buf factory is supplied to a factory
   * method.
   *
   * @return the default data buf factory of this factory.
   */
  @NonNull DataBufFactory defaultDataBufFactory();

  /**
   * Constructs a new rpc sender for the given class.
   *
   * @param component the associated network component, or null if not associated.
   * @param clazz     the class which the handler should handler.
   * @return a new rpc sender targeting the given class.
   * @throws NullPointerException if the given target class is null.
   */
  @NonNull RPCSender providerForClass(@Nullable NetworkComponent component, @NonNull Class<?> clazz);

  /**
   * Constructs a new rpc sender for the given class.
   *
   * @param component      the associated network component, or null if not associated.
   * @param clazz          the class which the handler should handler.
   * @param objectMapper   the object mapper to use for argument (de-) serialization.
   * @param dataBufFactory the data buf factory to use for buffer allocation.
   * @return a new rpc sender targeting the given class.
   * @throws NullPointerException if either the given class, mapper or buffer factory is null.
   */
  @NonNull RPCSender providerForClass(
    @Nullable NetworkComponent component,
    @NonNull Class<?> clazz,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory);

  @NonNull <T> T generateRPCBasedApi(
    @NonNull Class<T> baseClass,
    @Nullable NetworkComponent component);

  @NonNull <T> T generateRPCBasedApi(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @Nullable NetworkComponent component);

  @NonNull <T> T generateRPCBasedApi(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @Nullable NetworkComponent component,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory);

  /**
   * Constructs a new rpc handler for the given class.
   *
   * @param clazz   the class which the handler handles.
   * @param binding an optional instance binding for the handler, can be null if relying on contextual instances.
   * @return a new rpc handler for the given class.
   * @throws NullPointerException if the given target class is null.
   */
  @NonNull RPCHandler newHandler(@NonNull Class<?> clazz, @Nullable Object binding);

  /**
   * Constructs a new rpc handler for the given class.
   *
   * @param clazz          the class which the handler handles.
   * @param binding        an optional instance binding for the handler, null if relying on contextual instances.
   * @param objectMapper   the object mapper to use for argument (de-) serialization.
   * @param dataBufFactory the data buf factory to use for buffer allocation.
   * @return a new rpc handler for the given class.
   * @throws NullPointerException if either the given class, mapper or buffer factory is null.
   */
  @NonNull RPCHandler newHandler(
    @NonNull Class<?> clazz,
    @Nullable Object binding,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory);
}
