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

import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * A chainable rpc is a powerful extension to the normal rpc system which allows a developer to execute code on the
 * result of the previous method call. For example, the first rpc call might return an integer from the method
 * {@code availableDiskSpace}, you can then join on the integer of the method result and call {@code toString} directly
 * on the result of that method.
 * <p>
 * A rpc chain is completely null aware, you can call a method in the chain which returns null and the handler will
 * ensure that the result send back to the network component matches the expected method result, either returning null
 * for a null-accepting return type or the default value of a primitive type (for example an integer will be normalized
 * to 0).
 * <p>
 * Exceptions that happen on the way through the chain will get serialized and sent back to the calling network
 * component resulting in an exception on the caller site as well.
 * <p>
 * RPC chains are completely safe to re-use, every join on a rpc chain will result in a new rpc chain instance.
 *
 * @since 4.0
 */
public interface ChainableRPC extends RPCExecutable, RPCProvider {

  /**
   * Adds a join statement to the current rpc chain. This method always returns a new rpc chain with the given rpc as
   * the last rpc. Therefore, the result type expectation when firring the rpc chain is based on the last rpc given to
   * the chain using this method.
   * <p>
   * A chain can be re-used infinite times and even joined on without making a change to the original rpc chain.
   *
   * @param rpc the rpc to join on the result of the current chain.
   * @return a new rpc chain instance with the given rpc as its head rpc.
   * @throws NullPointerException if the given rpc is null.
   */
  @NonNull
  @Contract("_ -> new")
  RPCChain join(@NonNull RPC rpc);
}
