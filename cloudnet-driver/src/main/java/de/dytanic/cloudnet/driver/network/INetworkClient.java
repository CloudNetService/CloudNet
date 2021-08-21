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
 * Represents a client in the network which connect to a network server.
 */
public interface INetworkClient extends INetworkComponent, AutoCloseable {

  /**
   * Connects this network client to a network server.
   *
   * @param hostAndPort the target host to which the client should connect.
   * @return {@code true} if the connection was established successfully, {@code false} otherwise.
   */
  boolean connect(@NotNull HostAndPort hostAndPort);
}
