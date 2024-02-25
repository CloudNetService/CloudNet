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

import java.lang.reflect.Type;
import lombok.NonNull;

/**
 * The basic rpc class. A rpc holds all information about what should be called on the receiver site of the rpc and
 * which information (if any) should get sent back when the current rpc expects a result. Exceptions are handled during
 * the process and will be rethrown on the sender side rather than the receiver site.
 * <p>
 * The target method which should get called by the rpc is identified in 3 steps:
 * <ol>
 *   <li>Using the supplied target class of the rpc.
 *   <li>Using the name of the method supplied to the rpc.
 *   <li>Using the amount of arguments supplied to the rpc.
 * </ol>
 * <p>
 * By default, if on all network components is the same version of a class available there is no way a method can not
 * be identified by rpc as the creation of a sender based on a class with unclear methods will instantly result in an
 * exception to prevent exceptions during the runtime.
 *
 * @since 4.0
 */
public interface RPC extends RPCProvider, RPCExecutable, ChainableRPC {

  /**
   * Get the sender from which the rpc call is coming (better known as the base class in which the method to call by
   * this rpc is located in).
   *
   * @return the sender of this rpc.
   */
  @NonNull RPCSender sender();

  /**
   * Get the fully qualified name of the class the target method is located in.
   *
   * @return the fully qualified name of the class the target method is located in.
   */
  @NonNull String className();

  /**
   * Get the name of the method which should get called.
   *
   * @return the name of the method which should get called.
   */
  @NonNull String methodName();

  /**
   * Get the arguments which should get used to call the method. The main identification of a method happens based on
   * the name and the amount of arguments supplied. Invalid arguments might cause exceptions on the receiver site.
   *
   * @return the arguments which should get used to call the method.
   */
  @NonNull Object[] arguments();

  /**
   * Get the expected result type of the method invocation. Can be void.
   *
   * @return the expected result type of the method invocation.
   */
  @NonNull Type expectedResultType();

  /**
   * Disables that this rpc is waiting for a result from the target network component. By default, the current component
   * will always wait for the target method to be executed on the receiver site, event if the method returns void.
   *
   * @return the same instance used to call the method, for chaining.
   */
  @NonNull RPC disableResultExpectation();

  /**
   * Get if this rpc expects a result from the network component the invoke request is sent to.
   *
   * @return true if the current rpc expects a result from the target network component, false otherwise.
   */
  boolean expectsResult();
}
