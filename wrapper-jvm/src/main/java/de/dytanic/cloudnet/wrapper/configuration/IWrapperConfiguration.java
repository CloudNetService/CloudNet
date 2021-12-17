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

package de.dytanic.cloudnet.wrapper.configuration;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * The configuration mirror of the .wrapper/wrapper.json file in the working folder from the service process. It
 * includes all important data for the Wrapper to connect with the local node.
 * <p>
 * The configuration style is defined in the specific implementation of this interface
 */
public interface IWrapperConfiguration {

  /**
   * The key, for the connection, to authenticate with the provided node and the network
   *
   * @return the string which includes the key
   */
  @NotNull String connectionKey();

  /**
   * The address of a listener by the node
   *
   * @return the target local listener address of the node
   */
  @NotNull HostAndPort targetListener();

  /**
   * The first own serviceConfiguration with all important information about the service
   *
   * @return the instance of the ServiceConfiguration class
   */
  @NotNull ServiceConfiguration serviceConfiguration();

  /**
   * The first serviceInfoSnapshot which has the important data for all next serviceInfoSnapshots, which the wrapper has
   * to create
   *
   * @return the serviceInfoSnapshot sample
   */
  @NotNull ServiceInfoSnapshot serviceInfoSnapshot();

  /**
   * The ssl configuration, which needs for an optional ssl client connection.
   *
   * @return the configuration instance, which includes all important ssl settings, for a client ssl connection
   */
  @NotNull SSLConfiguration sslConfiguration();
}
