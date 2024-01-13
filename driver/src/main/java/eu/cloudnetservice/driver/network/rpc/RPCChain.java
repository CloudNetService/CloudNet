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

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents a chain of rpc calls, each of them behaves exactly as described in {@link RPC} and {@link ChainableRPC}.
 *
 * @since 4.0
 */
public interface RPCChain extends RPCProvider, RPCExecutable, ChainableRPC {

  /**
   * Get the current last call of the chain (the head). All result expectations are based on this rpc.
   *
   * @return the last call in the rpc chain.
   */
  @NonNull RPC head();

  /**
   * Get all joins of the rpc in the order in which they get called. The first call in the chain will be the first
   * element of this collection (method call order).
   *
   * @return all joins of the rpc in the order in which they get called.
   */
  @NonNull
  @UnmodifiableView Collection<RPC> joins();
}
