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

package eu.cloudnetservice.driver.impl.network.rpc.generation;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.impl.network.rpc.sender.DefaultRPCSender;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import java.lang.invoke.TypeDescriptor;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a rpc implementation builder.
 *
 * @param <T> the type that is being implemented.
 * @since 4.0
 */
public final class DefaultRPCImplementationBuilder<T> implements RPCImplementationBuilder<T> {

  private final RPCFactory sourceFactory;
  private final RPCClassMetadata classMetadata;
  private final RPCClassMetadata fullClasMetadata;
  private final RPCGenerationCache generationCache;

  private int generationFlags = 0x00;

  private ObjectMapper objectMapper;
  private Class<?> senderTargetClass;
  private DataBufFactory dataBufFactory;
  private Supplier<NetworkChannel> channelSupplier;

  /**
   * Constructs a new default implementation builder instance.
   *
   * @param classMetadata   the metadata of class that is being implemented.
   * @param generationCache the cache for all generated implementations.
   * @throws NullPointerException if the given class meta, sender builder or generation cache is null.
   */
  public DefaultRPCImplementationBuilder(
    @NonNull RPCFactory sourceFactory,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory,
    @NonNull RPCClassMetadata classMetadata,
    @NonNull RPCGenerationCache generationCache
  ) {
    this.sourceFactory = sourceFactory;
    this.classMetadata = classMetadata;
    this.fullClasMetadata = classMetadata.freeze(); // copy as immutable to not reflect changes into this
    this.generationCache = generationCache;
    this.objectMapper = objectMapper;
    this.dataBufFactory = dataBufFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> superclass(@NonNull Class<? super T> superclass) {
    Preconditions.checkArgument(superclass.isAssignableFrom(this.classMetadata.targetClass()));
    this.senderTargetClass = superclass;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> targetComponent(@NonNull NetworkComponent networkComponent) {
    return this.targetChannel(networkComponent::firstChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> targetChannel(@NonNull NetworkChannel channel) {
    return this.targetChannel(() -> channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> targetChannel(@NonNull Supplier<NetworkChannel> channelSupplier) {
    this.channelSupplier = channelSupplier;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> implementConcreteMethods() {
    this.generationFlags |= RPCImplementationGenerator.FLAG_IMPLEMENT_CONCRETE;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> excludeMethod(
    @NonNull String name,
    @NonNull TypeDescriptor methodDescriptor
  ) {
    // unregister from sender & our target meta
    this.classMetadata.unregisterMethod(name, methodDescriptor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> objectMapper(@NonNull ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCImplementationBuilder<T> dataBufFactory(@NonNull DataBufFactory dataBufFactory) {
    this.dataBufFactory = dataBufFactory;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull InstanceAllocator<T> generateImplementation() {
    Preconditions.checkState(this.channelSupplier != null, "channel supplier must be given");

    // build the rpc sender to use for the generated implementation
    var classMeta = this.classMetadata.freeze(); // immutable & copied - changes no longer reflect into it
    var senderTarget = Objects.requireNonNullElse(this.senderTargetClass, classMeta.targetClass());
    var sender = new DefaultRPCSender(
      senderTarget,
      this.sourceFactory,
      this.objectMapper,
      this.dataBufFactory,
      classMeta,
      () -> null);

    // generate the implementation
    var factory = this.generationCache.getOrGenerateImplementation(this.generationFlags, sender, this.fullClasMetadata);
    return new DefaultInstanceAllocator<>(null, sender, this.channelSupplier, new Object[0], factory);
  }

  /**
   * The default implementation of an instance allocator using an internal instance factory as delegation.
   *
   * @param baseRPC                         the base rpc to use for the constructed rpc implementation.
   * @param classRPCSender                  the rpc sender to use for the generated class.
   * @param channelSupplier                 thw channel supplier to use for rpc executions.
   * @param additionalConstructorParameters the additional super constructor parameters.
   * @param internalInstanceFactory         the internal instance factory to delegate to.
   * @param <T>                             the type that is being constructed by the allocator.
   * @since 4.0
   */
  private record DefaultInstanceAllocator<T>(
    @Nullable ChainableRPC baseRPC,
    @NonNull RPCSender classRPCSender,
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @NonNull Object[] additionalConstructorParameters,
    @NonNull RPCInternalInstanceFactory internalInstanceFactory
  ) implements InstanceAllocator<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InstanceAllocator<T> withBaseRPC(@Nullable ChainableRPC baseRPC) {
      return new DefaultInstanceAllocator<>(
        baseRPC,
        this.classRPCSender,
        this.channelSupplier,
        this.additionalConstructorParameters,
        this.internalInstanceFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InstanceAllocator<T> withTargetChannel(@NonNull Supplier<NetworkChannel> channelSupplier) {
      return new DefaultInstanceAllocator<>(
        this.baseRPC,
        this.classRPCSender,
        channelSupplier,
        this.additionalConstructorParameters,
        this.internalInstanceFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InstanceAllocator<T> withAdditionalConstructorParameters(Object... additionalConstructorParams) {
      Objects.requireNonNull(additionalConstructorParams, "additionalConstructorParams");
      return new DefaultInstanceAllocator<>(
        this.baseRPC,
        this.classRPCSender,
        this.channelSupplier,
        additionalConstructorParams.clone(),
        this.internalInstanceFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InstanceAllocator<T> changeConstructorParameter(int index, Object newConstructorParameter) {
      // don't do anything if the parameter type is already the same
      if (this.additionalConstructorParameters[index] == newConstructorParameter) {
        return this;
      }

      var constructorParams = this.additionalConstructorParameters.clone();
      constructorParams[index] = newConstructorParameter;
      return new DefaultInstanceAllocator<>(
        this.baseRPC,
        this.classRPCSender,
        this.channelSupplier,
        constructorParams,
        this.internalInstanceFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InstanceAllocator<T> insertConstructorParameters(int index, Object... parameters) {
      // validate the index where the insertion should begin
      var currentParamCount = this.additionalConstructorParameters.length;
      if (index < 0 || index > currentParamCount) {
        throw new IndexOutOfBoundsException(index);
      }

      // no need to do anything if nothing should be inserted into the array
      var paramCountToInsert = parameters.length;
      if (paramCountToInsert == 0) {
        return this;
      }

      // copy over the old parameters from the start to the given start index
      var newParams = new Object[currentParamCount + paramCountToInsert];
      if (index > 0) {
        System.arraycopy(this.additionalConstructorParameters, 0, newParams, 0, index);
      }

      // copy over the new parameters
      System.arraycopy(parameters, 0, newParams, index, paramCountToInsert);

      // copy over the old parameters if the index does not exceed the length of the current array
      if (index < currentParamCount) {
        System.arraycopy(
          this.additionalConstructorParameters,
          index,
          newParams,
          index + paramCountToInsert,
          currentParamCount - index);
      }

      return new DefaultInstanceAllocator<>(
        this.baseRPC,
        this.classRPCSender,
        this.channelSupplier,
        newParams,
        this.internalInstanceFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InstanceAllocator<T> appendConstructorParameters(Object... parameters) {
      return this.insertConstructorParameters(this.additionalConstructorParameters.length, parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public @NonNull T allocate() {
      return (T) this.internalInstanceFactory.constructInstance(
        this.baseRPC,
        this.classRPCSender,
        this.channelSupplier,
        this.additionalConstructorParameters);
    }
  }
}
