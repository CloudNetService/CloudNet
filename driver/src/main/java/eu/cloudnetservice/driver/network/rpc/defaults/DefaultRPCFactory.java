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

package eu.cloudnetservice.driver.network.rpc.defaults;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.DefaultRPCImplementationBuilder;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.RPCGenerationCache;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.DefaultRPCHandlerBuilder;
import eu.cloudnetservice.driver.network.rpc.defaults.sender.DefaultRPCSenderBuilder;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * The default factory implementation for everything related to rpc.
 *
 * @since 4.0
 */
@Singleton
@Provides(RPCFactory.class)
public final class DefaultRPCFactory implements RPCFactory {

  private final ObjectMapper defaultObjectMapper;
  private final DataBufFactory defaultDataBufFactory;
  private final RPCGenerationCache rpcGenerationCache;

  /**
   * Constructs a new default rpc provider factory instance.
   *
   * @param defaultObjectMapper   the default object mapper to use if no object mapper is provided in factory calls.
   * @param defaultDataBufFactory the default data buf factory to use if no object mapper is provided in factory calls.
   * @throws NullPointerException if either the given object mapper or data buf factory is null.
   */
  @Inject
  public DefaultRPCFactory(
    @NonNull ObjectMapper defaultObjectMapper,
    @NonNull DataBufFactory defaultDataBufFactory
  ) {
    this.defaultObjectMapper = defaultObjectMapper;
    this.defaultDataBufFactory = defaultDataBufFactory;
    this.rpcGenerationCache = new RPCGenerationCache(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder newRPCSenderBuilder(@NonNull Class<?> target) {
    var classMetadata = RPCClassMetadata.introspect(target);
    return this.newRPCSenderBuilder(classMetadata);
  }

  /**
   * Internal util to construct a new rpc sender builder from the given resolved class metadata.
   *
   * @param classMetadata the class metadata to use as a base for the builder.
   * @return a new rpc sender builder using the given class metadata.
   * @throws NullPointerException if the given class metadata is null.
   */
  @ApiStatus.Internal
  public @NonNull RPCSender.Builder newRPCSenderBuilder(@NonNull RPCClassMetadata classMetadata) {
    return new DefaultRPCSenderBuilder(this, classMetadata, this.defaultDataBufFactory, this.defaultObjectMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> RPCHandler.@NonNull Builder<T> newRPCHandlerBuilder(@NonNull Class<T> target) {
    var classMetadata = RPCClassMetadata.introspect(target);
    return new DefaultRPCHandlerBuilder<>(this, classMetadata, this.defaultObjectMapper, this.defaultDataBufFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> RPCImplementationBuilder<T> newRPCBasedImplementationBuilder(@NonNull Class<T> baseClass) {
    var classMeta = RPCClassMetadata.introspect(baseClass);
    var baseClassSenderBuilder = this.newRPCSenderBuilder(classMeta);
    return new DefaultRPCImplementationBuilder<>(classMeta, baseClassSenderBuilder, this.rpcGenerationCache);
  }
}
