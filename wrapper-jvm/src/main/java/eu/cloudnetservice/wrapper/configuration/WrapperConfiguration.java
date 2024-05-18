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

package eu.cloudnetservice.wrapper.configuration;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import jakarta.inject.Singleton;
import lombok.NonNull;

/**
 * Represents the configuration which is passed from a node instance to the wrapper in form of a file before starting.
 * It contains the most basic information in order to allow the wrapper to start and connect to the cluster.
 *
 * @since 4.0
 */
@Singleton
public interface WrapperConfiguration {

  /**
   * Get the key which allows this wrapper to authenticate with the associated node.
   *
   * @return the key which allows this wrapper to authenticate with the associated node.
   */
  @NonNull String connectionKey();

  /**
   * Get the address of the node listener this wrapper should connect.
   *
   * @return the address of the node listener this wrapper should connect.
   */
  @NonNull HostAndPort targetListener();

  /**
   * Get the service configuration which was used to create the service associated with this wrapper instance.
   *
   * @return the base service configuration of the associated service.
   */
  @NonNull ServiceConfiguration serviceConfiguration();

  /**
   * Get the initial service info snapshot created by the node before the wrapper was started.
   *
   * @return the initial service info snapshot created by the node before the wrapper was started.
   */
  @NonNull ServiceInfoSnapshot serviceInfoSnapshot();

  /**
   * Get the ssl configuration which should be applied when connecting and communicating with the associated node.
   *
   * @return the ssl configuration to apply to all client connection of this wrapper.
   */
  @NonNull SSLConfiguration sslConfiguration();
}
