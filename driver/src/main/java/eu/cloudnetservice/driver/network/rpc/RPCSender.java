/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.rpc.exception.CannotDecideException;
import lombok.NonNull;

/**
 * A sender which can send rpc requests to another network component.
 *
 * @since 4.0
 */
public interface RPCSender extends RPCProvider {

  /**
   * Get the provider factory used to construct this rpc sender.
   *
   * @return the provider factory used to construct this rpc sender.
   */
  @NonNull RPCFactory factory();

  /**
   * Get the network component associated with this rpc sender.
   *
   * @return the network component associated with this rpc sender.
   * @throws UnsupportedOperationException if this sender has no network component associated.
   */
  @NonNull NetworkComponent associatedComponent();

  /**
   * Creates a rpc to invoke the target method with no arguments, preventing a new object array allocation each time the
   * method is invoked. Equivalent to {@code sender.invokeMethod(name, new Object[0])}.
   *
   * @param methodName the name of the method to invoke.
   * @return the rpc usable to fire the rpc request against the given method.
   * @throws NullPointerException  if the given method name is null.
   * @throws CannotDecideException if either none or multiple methods are matching the given method name.
   */
  @NonNull RPC invokeMethod(@NonNull String methodName);

  /**
   * Creates a rpc to invoke the target method with the given arguments. For this operation to succeed, the class must
   * <ol>
   *   <li>contain a method which equals the given method name (case sensitive)
   *   <li>that method must have the amount of arguments supplied via the arguments array.
   * </ol>
   *
   * @param methodName the name of the method to invoke.
   * @param args       the arguments to use for the method invocation.
   * @return the rpc usable to fire the rpc request against the given method.
   * @throws NullPointerException  if the given method name is null.
   * @throws CannotDecideException if either none or multiple methods are matching the given method name.
   */
  @NonNull RPC invokeMethod(@NonNull String methodName, Object... args);
}
