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

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkComponent;
import java.lang.invoke.TypeDescriptor;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * A sender which can send rpc requests to another network component.
 *
 * @since 4.0
 */
public interface RPCSender extends RPCProvider {

  /**
   * Returns a new RPC that can invoke the calling method with the provided arguments.
   *
   * @param args the arguments to supply when invoking the method.
   * @return an RPC that calls the caller method on a remote component with the provided arguments.
   * @throws IllegalStateException    if the calling method cannot be resolved.
   * @throws IllegalArgumentException if the calling method is not known in the target class.
   */
  @NonNull
  RPC invokeCaller(Object... args);

  /**
   * Returns a new RPC that can invoke the calling method with the provided arguments.
   *
   * @param callerStackOffset the offset on the stack where the target method is located.
   * @param args              the arguments to supply when invoking the method.
   * @return an RPC that calls the caller method on a remote component with the provided arguments.
   * @throws IllegalStateException    if the calling method cannot be resolved.
   * @throws IllegalArgumentException if the calling method is not known in the target class.
   */
  @NonNull
  RPC invokeCallerWithOffset(int callerStackOffset, Object... args);

  /**
   * Tries to resolve a distinct method with the given name and argument count in the target class and returns a new RPC
   * that can invoke the resolved method with the provided arguments.
   *
   * @param methodName the name of the method to invoke.
   * @param args       the arguments to supply when invoking the method.
   * @return an RPC to invoke the resolved method with the given name and parameter count on the remote.
   * @throws NullPointerException     if the given method name or arguments array is null.
   * @throws IllegalArgumentException if the method to call cannot be resolved in the target class.
   */
  @NonNull
  RPC invokeMethod(@NonNull String methodName, Object... args);

  /**
   * Returns a new RPC that can invoke the method with the given name and descriptor.
   *
   * @param methodName the name of the method to invoke.
   * @param methodDesc the descriptor of the method to invoke.
   * @param args       the arguments to supply for the method invocation.
   * @return an RPC to invoke the resolved method with the given parameters on a remote.
   * @throws NullPointerException     if the given method name, method descriptor or argument array is null.
   * @throws IllegalArgumentException if the method to call cannot be resolved in the target class.
   */
  @NonNull
  RPC invokeMethod(@NonNull String methodName, @NonNull TypeDescriptor methodDesc, Object... args);

  /**
   * A builder for an RPC sender which can be obtained from an RPC factory.
   *
   * @since 4.0
   */
  interface Builder extends RPCProvider.Builder<Builder> {

    /**
     * Sets the network component to use for obtaining the network channel to send RPCs using this sender to. For that
     * purpose the first channel of the given component is always used. An exception during RPC execution will be thrown
     * if the first channel of the component every becomes null or closed.
     *
     * @param networkComponent the network component to use the first channel of during RPC execution.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given network component is null.
     */
    @NonNull
    @Contract("_ -> this")
    Builder targetComponent(@NonNull NetworkComponent networkComponent);

    /**
     * Sets the singleton channel that will be used to send all RPCs that are using this sender to. The target channel
     * should not be closed while this sender is in use.
     *
     * @param channel the channel to which all RPCs that are using this sender are sent to.
     * @return this builder, for chaining.
     * @throws NullPointerException     if the given channel is null.
     * @throws IllegalArgumentException if the given channel is closed.
     */
    @NonNull
    @Contract("_ -> this")
    Builder targetChannel(@NonNull NetworkChannel channel);

    /**
     * Sets the supplier to use to resolve the network channel to which RPCs using this sender should be sent. The given
     * supplier is called every time the channel is needed, and is allowed to change over time. An exception during RPC
     * execution will be thrown if this supplier ever returns null or a channel that is closed.
     *
     * @param channelSupplier the supplier to call to obtain the target channel for RPCs.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given supplier is null.
     */
    @NonNull
    @Contract("_ -> this")
    Builder targetChannel(@NonNull Supplier<NetworkChannel> channelSupplier);

    /**
     * Excludes the method in the target class that has the given name and method descriptor from being discovered for
     * RPC execution. The method will not be callable using the sender and will not be introspected during build.
     *
     * @param name             the name of the method to exclude.
     * @param methodDescriptor the descriptor of the method to exclude.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given name or method descriptor is null.
     */
    @NonNull
    @Contract("_, _ -> this")
    Builder excludeMethod(@NonNull String name, @NonNull TypeDescriptor methodDescriptor);

    /**
     * Validates the options and target class provided to this builder and constructs the final sender instance in case
     * everything checks out.
     *
     * @return a new sender instance based on the options provided to this builder.
     * @throws IllegalArgumentException if an option provided to this builder does not match.
     */
    @NonNull
    @Contract("-> new")
    RPCSender build();
  }
}
