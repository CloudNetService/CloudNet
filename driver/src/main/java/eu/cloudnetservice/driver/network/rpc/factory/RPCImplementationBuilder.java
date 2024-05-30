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

import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface RPCImplementationBuilder {

  /**
   * Sets the class that should be extended by the generated class. Note that this method can only be used if the
   * provided class when obtaining this builder was an interface.
   *
   * @param classToExtend the class that should be extended by the generated class, can be null to not extend anything.
   * @return this builder for chaining.
   * @throws IllegalStateException    if this builder was constructed based on a concrete class that will be extended.
   * @throws IllegalArgumentException if the given class is not concrete (for example an interface).
   */
  @NonNull
  @Contract("_ -> this")
  RPCImplementationBuilder extend(@Nullable Class<?> classToExtend);

  /**
   * Sets the interfaces that should be implemented. If this builder was constructed for an interface class, the given
   * interfaces are implemented in addition to the base interface.
   *
   * @param interfaces the interfaces that should be implemented.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given array or one element of the array is null.
   */
  @NonNull
  @Contract("_ -> this")
  RPCImplementationBuilder implement(@NonNull Class<?>... interfaces);

  /**
   * Returns a configuration step for a specific method in the build process. This can be used to specifically configure
   * how RPC should be handled with the method. This behaviour can also be configured using annotations. However, if
   * present, the configuration made from this builder will override any annotations on a method.
   *
   * @param name             the name of the method that should be configured.
   * @param methodDescriptor the descriptor of the method that should be configured.
   * @return a configuration step for a method in
   */
  @NonNull
  @Contract("_, _ -> new")
  RPCImplementationMethodConfigurator configureMethod(@NonNull String name, @NonNull MethodTypeDesc methodDescriptor);

  /**
   * A sub-step builder for the RPC implementation which allows to explicitly configure how a method should be handled.
   *
   * @since 4.0
   */
  interface RPCImplementationMethodConfigurator {

    /**
     * Instructs the RPC implementation generator to completely skip the implementation of the target method. Note: this
     * is only possible to set if the target method is not abstract and already has an implementation. If the target
     * method is abstract, an error is thrown when trying to construct the RPC implementation.
     *
     * @return the builder instance that was used to obtain this decorator instance.
     */
    @NonNull
    RPCImplementationBuilder skip();

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
    RPCImplementationMethodConfigurator skipResultWait();

    /**
     * Applies the method configuration to the owning builder and returns the instance of the owning builder for further
     * configuration of the build process.
     *
     * @return the builder instance that was used to obtain this decorator instance.
     */
    @NonNull
    RPCImplementationBuilder apply();
  }
}
