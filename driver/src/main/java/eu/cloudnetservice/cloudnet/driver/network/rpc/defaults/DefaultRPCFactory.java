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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults;

import eu.cloudnetservice.cloudnet.driver.network.NetworkComponent;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.generation.ApiImplementationGenerator;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.sender.DefaultRPCSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default factory implementation for everything related to rpc.
 *
 * @since 4.0
 */
public class DefaultRPCFactory implements RPCFactory {

  protected final ObjectMapper defaultObjectMapper;
  protected final DataBufFactory defaultDataBufFactory;

  /**
   * Constructs a new default rpc provider factory instance.
   *
   * @param defaultObjectMapper   the default object mapper to use if no object mapper is provided in factory calls.
   * @param defaultDataBufFactory the default data buf factory to use if no object mapper is provided in factory calls.
   * @throws NullPointerException if either the given object mapper or data buf factory is null.
   */
  public DefaultRPCFactory(
    @NonNull ObjectMapper defaultObjectMapper,
    @NonNull DataBufFactory defaultDataBufFactory
  ) {
    this.defaultObjectMapper = defaultObjectMapper;
    this.defaultDataBufFactory = defaultDataBufFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ObjectMapper defaultObjectMapper() {
    return this.defaultObjectMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBufFactory defaultDataBufFactory() {
    return this.defaultDataBufFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender providerForClass(@Nullable NetworkComponent component, @NonNull Class<?> clazz) {
    return this.providerForClass(component, clazz, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender providerForClass(
    @Nullable NetworkComponent component,
    @NonNull Class<?> clazz,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    return new DefaultRPCSender(this, component, clazz, objectMapper, dataBufFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull T generateRPCBasedApi(@NonNull Class<T> baseClass, @Nullable NetworkComponent component) {
    return this.generateRPCBasedApi(baseClass, GenerationContext.forClass(baseClass).build(), component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull T generateRPCBasedApi(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @Nullable NetworkComponent component
  ) {
    return this.generateRPCBasedApi(
      baseClass,
      context,
      component,
      this.defaultObjectMapper,
      this.defaultDataBufFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @NonNull T generateRPCBasedApi(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @Nullable NetworkComponent component,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    var sender = this.providerForClass(component, baseClass, objectMapper, dataBufFactory);
    return ApiImplementationGenerator.generateApiImplementation(baseClass, context, sender);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCHandler newHandler(@NonNull Class<?> clazz, @Nullable Object binding) {
    return this.newHandler(clazz, binding, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCHandler newHandler(
    @NonNull Class<?> clazz,
    @Nullable Object binding,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    return new DefaultRPCHandler(clazz, binding, objectMapper, dataBufFactory);
  }
}
