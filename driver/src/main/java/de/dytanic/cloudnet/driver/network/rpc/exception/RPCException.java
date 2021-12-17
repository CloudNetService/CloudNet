/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.exception;

import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCChain;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class RPCException extends RuntimeException {

  public RPCException(@NotNull RPC rpc, @NotNull Exception root) {
    super(String.format("Unable to get yield result of rpc: %s", formatRPC(rpc)), root);
  }

  public RPCException(@NotNull RPCChain chain, @NotNull Exception root) {
    super(String.format(
      "Unable to get future result of chained rpc; stack: %s\n%s",
      formatRPC(chain.head()),
      chain.joins().stream()
        .map(RPCException::formatChainedRPCEntry)
        .collect(Collectors.joining("\n"))), root);
  }

  protected static @NotNull String formatRPC(@NotNull RPC rpc) {
    return String.format("%s.%s(%s)", rpc.className(), rpc.methodName(), Arrays.toString(rpc.arguments()));
  }

  protected static @NotNull String formatChainedRPCEntry(@NotNull RPC rpc) {
    return String.format(" at %s", formatRPC(rpc));
  }
}
