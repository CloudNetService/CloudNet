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

package de.dytanic.cloudnet.driver.network;

import org.jetbrains.annotations.NotNull;

/**
 * The network client represents a client based connector to one or more remote servers.
 */
public interface INetworkClient extends INetworkComponent, AutoCloseable {

  /**
   * Open a new connection to the specific host and port
   *
   * @param hostAndPort the address, that should the client connect to
   * @return true if the connection was success or false if the connection was unsuccessful or refused
   */
  boolean connect(@NotNull HostAndPort hostAndPort);

  /**
   * Gets the time when this client was connected to the server in milliseconds
   */
  long getConnectedTime();

}
