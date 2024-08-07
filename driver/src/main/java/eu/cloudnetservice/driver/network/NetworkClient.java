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

package eu.cloudnetservice.driver.network;

import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

/**
 * Represents a network component which can be connected to any network server.
 *
 * @since 4.0
 */
public interface NetworkClient extends NetworkComponent, AutoCloseable {

  /**
   * Connects this network client to the network server running at the given host and port.
   *
   * @param hostAndPort the target host and port to which the client should get connected.
   * @return a task completed successfully, or with the exception thrown during the connection process.
   * @throws NullPointerException if the given host and port is null.
   */
  @NonNull
  CompletableFuture<Void> connect(@NonNull HostAndPort hostAndPort);
}
