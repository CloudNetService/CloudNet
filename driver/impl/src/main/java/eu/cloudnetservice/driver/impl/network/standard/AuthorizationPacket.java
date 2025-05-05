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

package eu.cloudnetservice.driver.impl.network.standard;

import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import lombok.NonNull;

/**
 * The packet which is used for authorization between:
 * <ul>
 *   <li>A wrapper and a node.
 *   <li>A node and a node.
 * </ul>
 * <p>
 * This packet holds all information which is required for a node to determine all information needed for a successful
 * authentication. When authenticating a wrapper with a node this packet contains:
 * <ol>
 *   <li>The connection key assigned to the service by the node when starting the service.
 *   <li>The service id of the service which connects to the node.
 * </ol>
 * <p>
 * When authenticating a node with a node this packet contains:
 * <ol>
 *   <li>The cluster id
 *   <li>The network cluster node (offline information) of the node.
 * </ol>
 *
 * @since 4.0
 */
public final class AuthorizationPacket extends BasePacket {

  /**
   * Constructs a new authorization packet.
   *
   * @param type    the requested type of authorization.
   * @param dataBuf the data for the authorization which are required for the given type.
   * @throws NullPointerException if either the type or data buf is null.
   */
  public AuthorizationPacket(@NonNull PacketAuthorizationType type, @NonNull DataBuf dataBuf) {
    super(
      NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
      DataBuf.empty().writeObject(type).writeDataBuf(dataBuf));
  }

  /**
   * The type of authorization used to determine which type of component is connecting and which information is required
   * for a successful auth.
   *
   * @since 4.0
   */
  public enum PacketAuthorizationType {

    /**
     * The authorization type for a node which should authenticate another node.
     */
    NODE_TO_NODE,
    /**
     * The authorization type for a node which should authenticate a wrapper (or a running service in other words).
     */
    WRAPPER_TO_NODE
  }
}
