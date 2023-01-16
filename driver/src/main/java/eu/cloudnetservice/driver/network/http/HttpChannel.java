/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import lombok.NonNull;

/**
 * A http channel represented in the most basic form. The channel only provides basic information about the connection
 * to the client and allows to close it.
 *
 * @since 4.0
 */
public interface HttpChannel extends AutoCloseable {

  /**
   * Get the host and port of the server binding the client is connected to.
   *
   * @return the host and port of the server.
   */
  @NonNull HostAndPort serverAddress();

  /**
   * Get the host and port of the client which is connected to the server.
   *
   * @return the host and port of the client.
   */
  @NonNull HostAndPort clientAddress();

  /**
   * Closes the underlying connection of the client to the server. After the close this channel cannot be used anymore.
   */
  @Override
  void close();
}
