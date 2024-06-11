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
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCProvider;
import eu.cloudnetservice.driver.network.rpc.generation.ChainInstanceFactory;
import eu.cloudnetservice.driver.network.rpc.generation.InstanceFactory;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * A builder for an implementation of a class that will use RPC for all method calls.
 *
 * @param <T> the type that is being implemented by the build process.
 * @since 4.0
 */
public interface RPCImplementationBuilder<T, B extends RPCImplementationBuilder<T, B>> extends RPCProvider.Builder<B> {

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
  B targetComponent(@NonNull NetworkComponent networkComponent);

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
  B targetChannel(@NonNull NetworkChannel channel);

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
  B targetChannel(@NonNull Supplier<NetworkChannel> channelSupplier);

  /**
   * Enables that concrete methods (methods that are non-abstract and already have an implementation in the extending
   * class or interfaces) should be implemented with RPC calls anyway.
   *
   * @return this builder, for chaining.
   */
  @NonNull
  @Contract("-> this")
  B implementConcreteMethods();

  /**
   * Returns a configuration step for a specific method in the build process. This can be used to specifically configure
   * how RPC should be handled with the method. This behaviour can also be configured using annotations. However, if
   * present, the configuration made from this builder will override any annotations on a method.
   *
   * @param name       the name of the method that should be configured.
   * @param methodDesc the descriptor of the method that should be configured.
   * @return a configuration step for a method in
   */
  @NonNull
  @Contract("_, _ -> new")
  RPCImplementationMethodConfigurator<B> configureMethod(@NonNull String name, @NonNull MethodType methodDesc);

  /**
   * A sub-step builder for the RPC implementation which allows to explicitly configure how a method should be handled.
   *
   * @param <B> the type of the builder that is being returned to.
   * @since 4.0
   */
  interface RPCImplementationMethodConfigurator<B> {

    /**
     * Instructs the RPC implementation generator to completely skip the implementation of the target method. Note: this
     * is only possible to set if the target method is not abstract and already has an implementation. If the target
     * method is abstract, an error is thrown when trying to construct the RPC implementation.
     *
     * @return the builder instance that was used to obtain this decorator instance.
     */
    @NonNull
    B skip();

    /**
     * Instructs the RPC generator to not await the execution of the target method on the remote side. Usually when
     * executing a method via RPC, a result is awaited even if the method returns {@code void}. This can be skipped when
     * this option is enabled, leading to the fact that the RPC is just sent to the remote side and the method returns
     * instantly.
     *
     * @return this configurator, for chaining.
     */
    @NonNull
    @Contract("-> this")
    RPCImplementationMethodConfigurator<B> skipResultWait();

    /**
     * Applies the method configuration to the owning builder and returns the instance of the owning builder for further
     * configuration of the build process.
     *
     * @return the builder instance that was used to obtain this decorator instance.
     */
    @NonNull
    B apply();
  }

  /**
   * The builder implementation for generating a basic RPC implementation.
   *
   * @param <T> the type that is being implemented.
   * @since 4.0
   */
  interface ForBasic<T> extends RPCImplementationBuilder<T, ForBasic<T>> {

    /**
     * Constructs the basic RPC class implementation based on the options provided to the builder.
     *
     * @return a new RPC implementation based on the provided options.
     * @throws IllegalArgumentException if one of the options is invalid.
     */
    @NonNull
    @Contract("-> new")
    InstanceFactory<T> build();
  }

  /**
   * The builder implementation for generating a chained RPC implementation.
   *
   * @param <T> the type that is being implemented.
   * @since 4.0
   */
  interface ForChained<T> extends RPCImplementationBuilder<T, ForChained<T>> {

    /**
     * Sets the base RPC that must be executed, whose result will be used to execute the RPC calls in the target class.
     *
     * @param baseRpc the base rpc to execute to obtain the instance on which calls from the target should be executed.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given base rpc is null.
     */
    @NonNull
    @Contract("_ -> this")
    ForChained<T> baseRPC(@NonNull RPC baseRpc);

    /**
     * Constructs the chained RPC class implementation based on the options provided to the builder.
     *
     * @return a new chained RPC implementation based on the provided options.
     * @throws IllegalArgumentException if one of the options is invalid.
     */
    @NonNull
    @Contract("-> new")
    ChainInstanceFactory<T> build();
  }
}
