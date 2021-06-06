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
 * The network server represents a server that can register and receive INetworkClient connections and packets It is
 * made for a simple read and write network with a client and a server. You can bind this server on more than one
 * addresses
 *
 * @see INetworkClient
 */
public interface INetworkServer extends INetworkComponent, AutoCloseable {

  /**
   * Binds the server to a specific port with the host alias address "0.0.0.0"
   *
   * @param port the port, that the server should bind
   * @return true when the binding was successful or false if an error was threw or the port is already bind
   */
  boolean addListener(int port);

  /**
   * Binds the server to a specific address that is as parameter defined
   *
   * @param hostAndPort the address that should the server bind
   * @return true when the binding was successful or false if an error was threw or the port is already bind
   */
  boolean addListener(@NotNull HostAndPort hostAndPort);
}
