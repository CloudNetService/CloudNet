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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.ChainedApiImplementationGenerator;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.DefaultRPCHandler;
import eu.cloudnetservice.driver.network.rpc.defaults.sender.DefaultRPCSender;
import eu.cloudnetservice.driver.network.rpc.generation.ChainInstanceFactory;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.network.rpc.generation.InstanceFactory;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default factory implementation for everything related to rpc.
 *
 * @since 4.0
 */
@Singleton
@Provides(RPCFactory.class)
public class DefaultRPCFactory implements RPCFactory {

  protected final ObjectMapper defaultObjectMapper;
  protected final DataBufFactory defaultDataBufFactory;

  protected final Cache<GenerationContext, InstanceFactory<?>> generatedApiCache = Caffeine.newBuilder().build();
  protected final Table<String, GenerationContext, ChainInstanceFactory<?>> chainFactoryCache = HashBasedTable.create();

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
  @SuppressWarnings("unchecked")
  public <T> @NonNull InstanceFactory<T> generateRPCBasedApi(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context
  ) {
    return (InstanceFactory<T>) this.generatedApiCache.get(context, $ -> {
      var sender = this.senderFromGenerationContext(context, baseClass);
      return ApiImplementationGenerator.generateApiImplementation(baseClass, context, sender);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> ChainInstanceFactory<T> generateRPCChainBasedApi(
    @NonNull RPCSender baseSender,
    @NonNull Class<T> chainBaseClass,
    @NonNull GenerationContext context
  ) {
    return this.generateRPCChainBasedApi(
      baseSender,
      StackWalker.getInstance().walk(stream -> stream
        .skip(1)
        .map(StackWalker.StackFrame::getMethodName)
        .findFirst()
        .orElseThrow()),
      chainBaseClass,
      context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull <T> ChainInstanceFactory<T> generateRPCChainBasedApi(
    @NonNull RPCSender baseSender,
    @NonNull String baseCallerMethod,
    @NonNull Class<T> chainBaseClass,
    @NonNull GenerationContext context
  ) {
    // generate the instance factory if we need to
    var factory = (ChainInstanceFactory<T>) this.chainFactoryCache.get(baseCallerMethod, context);
    if (factory == null) {
      // not yet generated, generate and add it
      factory = ChainedApiImplementationGenerator.generateApiImplementation(
        chainBaseClass,
        context,
        this.senderFromGenerationContext(context, chainBaseClass),
        args -> baseSender.invokeMethod(baseCallerMethod, args));
      this.chainFactoryCache.put(baseCallerMethod, context, factory);
    }
    // return the cached or generated factory
    return factory;
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

  /**
   * Constructs a new rpc sender for the given base class and using the options supplied by the given generation
   * context.
   *
   * @param context the context to take out the needed options from.
   * @param base    the base class in which the new sender should call the methods.
   * @return a new rpc sender instance.
   * @throws NullPointerException if the given context or base class is null.
   */
  private @NonNull RPCSender senderFromGenerationContext(@NonNull GenerationContext context, @NonNull Class<?> base) {
    var objectMapper = Objects.requireNonNullElse(context.objectMapper(), this.defaultObjectMapper);
    var dataBufFactory = Objects.requireNonNullElse(context.dataBufFactory(), this.defaultDataBufFactory);
    // construct the sender
    return this.providerForClass(context.component(), base, objectMapper, dataBufFactory);
  }
}
