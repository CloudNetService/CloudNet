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

package eu.cloudnetservice.driver.network.rpc.factory;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPCProvider;
import java.lang.invoke.TypeDescriptor;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * A builder for an implementation of a class that will use RPC for all method calls.
 *
 * @param <T> the type that is being implemented by the build process.
 * @since 4.0
 */
public interface RPCImplementationBuilder<T> extends RPCProvider.Builder<RPCImplementationBuilder<T>> {

  /**
   * Sets the network component to use for obtaining the network channel to send RPCs to. For that purpose the first
   * channel of the given component is always used. An exception during RPC execution will be thrown if the first
   * channel of the component every becomes null or closed.
   *
   * @param networkComponent the network component to use the first channel of during RPC execution.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given network component is null.
   */
  @NonNull
  @Contract("_ -> this")
  RPCImplementationBuilder<T> targetComponent(@NonNull NetworkComponent networkComponent);

  /**
   * Sets the singleton channel that will be used to send all RPCs to. The target channel should not be closed while
   * this sender is in use.
   *
   * @param channel the channel to which all RPCs that are using this sender are sent to.
   * @return this builder, for chaining.
   * @throws NullPointerException     if the given channel is null.
   * @throws IllegalArgumentException if the given channel is closed.
   */
  @NonNull
  @Contract("_ -> this")
  RPCImplementationBuilder<T> targetChannel(@NonNull NetworkChannel channel);

  /**
   * Sets the supplier to use to resolve the network channel to which RPCs should be sent. The given supplier is called
   * every time the channel is needed, and is allowed to change over time. An exception during RPC execution will be
   * thrown if this supplier ever returns null or a channel that is closed.
   *
   * @param channelSupplier the supplier to call to obtain the target channel for RPCs.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given supplier is null.
   */
  @NonNull
  @Contract("_ -> this")
  RPCImplementationBuilder<T> targetChannel(@NonNull Supplier<NetworkChannel> channelSupplier);

  /**
   * Enables that concrete methods (methods that are non-abstract and already have an implementation in the extending
   * class or interfaces) should be implemented with RPC calls anyway.
   *
   * @return this builder, for chaining.
   */
  @NonNull
  @Contract("-> this")
  RPCImplementationBuilder<T> implementConcreteMethods();

  /**
   * Excludes the method in the target class that has the given name and method descriptor from being discovered for RPC
   * execution. The method will not be callable using the sender and will not be introspected during build.
   *
   * @param name             the name of the method to exclude.
   * @param methodDescriptor the descriptor of the method to exclude.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given name or method descriptor is null.
   */
  @NonNull
  @Contract("_, _ -> this")
  RPCImplementationBuilder<T> excludeMethod(@NonNull String name, @NonNull TypeDescriptor methodDescriptor);

  /**
   * Generates an implementation of the target class and returns an allocator which can be used to construct an instance
   * of the generated class.
   *
   * @return an allocator which can be used to construct an instance of the underlying class.
   * @throws IllegalStateException if one of the provided options is invalid.
   */
  @NonNull
  @Contract("-> new")
  InstanceAllocator<T> generateImplementation();

  /**
   * An allocator for instances of generated rpc implementations.
   *
   * @param <T> the type that is being constructed.
   * @since 4.0
   */
  interface InstanceAllocator<T> {

    /**
     * Returns a new instance allocator which uses the given base rpc when constructing the underlying implementation.
     *
     * @param baseRPC the base rpc to use for the rpc executions in the target class, can be null.
     * @return a new instance allocator instance which uses the given base rpc when allocating.
     */
    @NonNull
    @Contract("_ -> new")
    InstanceAllocator<T> withBaseRPC(@Nullable ChainableRPC baseRPC);

    /**
     * Returns a new instance allocator which uses the given channel supplier when constructing the underlying
     * implementation.
     *
     * @param channelSupplier the channel supplier to use as a target for all rpc calls.
     * @return a new instance allocator which uses the given channel supplier when allocating.
     * @throws NullPointerException if the given channel supplier is null.
     */
    @NonNull
    @Contract("_ -> new")
    InstanceAllocator<T> withTargetChannel(@NonNull Supplier<NetworkChannel> channelSupplier);

    /**
     * Sets the additional constructor parameters which should be passed to the super constructor when allocating an
     * instance of the generated api implementation.
     *
     * @param additionalConstructorParameters the additional constructor arguments to pass to the super constructor.
     * @return a new instance allocator which uses the given additional constructor parameters when allocating.
     * @throws NullPointerException if the given additional constructor parameters are null.
     */
    @NonNull
    @Contract("_ -> new")
    InstanceAllocator<T> withAdditionalConstructorParameters(Object... additionalConstructorParameters);

    /**
     * Allocates an instance of the generated implementation based on the provided arguments.
     *
     * @return an allocated instance of the implemented target class.
     * @throws IllegalStateException if the allocation is not possible.
     */
    @NonNull
    @Contract("-> new")
    T allocate();
  }
}
