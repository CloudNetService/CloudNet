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

import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * A builder for an RPC sender which can be obtained from an RPC factory.
 *
 * @since 4.0
 */
public interface RPCSenderBuilder {

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
  RPCSenderBuilder excludeMethod(@NonNull String name, @NonNull MethodTypeDesc methodDescriptor);

  /**
   * Sets the network component to use for the constructed sender. Can be null at build time to indicate that the
   * network component for communication is always explicitly provided when RPC methods are executed.
   *
   * @param networkComponent the network component to communicate with from the constructed sender, can be null.
   * @return this builder, for chaining.
   */
  @NonNull
  @Contract("_ -> this")
  RPCSenderBuilder networkComponent(@Nullable NetworkComponent networkComponent);

  /**
   * Validates the options and target class provided to this builder and constructs the final sender instance in case
   * everything checks out.
   *
   * @return a new sender instance based on the options provided to this builder.
   * @throws IllegalArgumentException if an option provided to this builder does not match.
   */
  @NonNull
  @Contract("-> new")
  RPCSender construct();
}
