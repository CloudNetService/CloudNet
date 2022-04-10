/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.http;

import eu.cloudnetservice.driver.network.HostAndPort;
import java.net.SocketAddress;
import lombok.NonNull;

/**
 * Represents a http component which can be bound, receive and handle http messages.
 *
 * @since 4.0
 */
public interface HttpServer extends HttpComponent<HttpServer> {

  /**
   * Adds a listener on the given port if a listener on the port does not exist already.
   *
   * @param port the to bind to.
   * @return true if the bind was successful, false otherwise.
   */
  boolean addListener(int port);

  /**
   * Adds a listener on the given socket address if the listener does not exist already.
   *
   * @param socketAddress the address to listen to.
   * @return true if the bind was successful, false otherwise.
   * @throws NullPointerException if the given socket address is null.
   */
  boolean addListener(@NonNull SocketAddress socketAddress);

  /**
   * Adds a listener on the given host and port if the listener does not exist already.
   *
   * @param hostAndPort the host and port to listen to.
   * @return true if the bind was successful, false otherwise.
   * @throws NullPointerException if the given host and port is null.
   */
  boolean addListener(@NonNull HostAndPort hostAndPort);
}
