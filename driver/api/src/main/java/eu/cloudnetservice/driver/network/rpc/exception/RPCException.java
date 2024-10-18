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

package eu.cloudnetservice.driver.network.rpc.exception;

import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * A runtime exception thrown when anything went wrong during the execution of a rpc or rpc chain.
 *
 * @since 4.0
 */
public class RPCException extends RuntimeException {

  /**
   * Constructs a new rpc exception instance.
   *
   * @param rpc  the rpc during which the execution happened.
   * @param root the exception which was thrown.
   * @throws NullPointerException if either the given rpc or root exception is null.
   */
  public RPCException(@NonNull RPC rpc, @NonNull Exception root) {
    super(String.format("Unable to get yield result of rpc: %s", formatRPC(rpc)), root);
  }

  /**
   * Constructs a new rpc exception instance.
   *
   * @param chain the rpc chain during which execution the execution happened.
   * @param root  the exception which was thrown.
   * @throws NullPointerException if either the given rpc chain or root exception is null.
   */
  public RPCException(@NonNull RPCChain chain, @NonNull Exception root) {
    super(String.format(
      "Unable to get future result of chained rpc; stack: %s\n%s",
      formatRPC(chain.tail()),
      chain.joins().stream()
        .map(RPCException::formatChainedRPCEntry)
        .collect(Collectors.joining("\n"))), root);
  }

  /**
   * Formats the given rpc into a better readable string, including the target class and method name as well as all
   * arguments which were used for the target method call.
   *
   * @param rpc the rpc chain during which the execution happened.
   * @return a formatted, human-readable string based on the given rpc.
   * @throws NullPointerException if the given rpc is null.
   */
  protected static @NonNull String formatRPC(@NonNull RPC rpc) {
    return String.format("%s.%s(%s)", rpc.className(), rpc.methodName(), Arrays.toString(rpc.arguments()));
  }

  /**
   * Formats the given rpc entry, using the {@code formatRPC} method and prepends a {@code at} to make it look like an
   * exception. This is more for easier reading which rpc entries were joined on.
   *
   * @param rpc the next entry in the rpc chain to format.
   * @return a formatted, human-readable string based on the given rpc entry.
   * @throws NullPointerException if the given rpc entry is null.
   */
  protected static @NonNull String formatChainedRPCEntry(@NonNull RPC rpc) {
    return String.format(" at %s", formatRPC(rpc));
  }
}
