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

package eu.cloudnetservice.cloudnet.wrapper.configuration;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.network.ssl.SSLConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * The default json based wrapper configuration for the service. It loads only the configuration with the constructor
 * all properties once.
 *
 * @see WrapperConfiguration
 */
public record DocumentWrapperConfiguration(
  @NonNull String connectionKey,
  @NonNull HostAndPort targetListener,
  @NonNull SSLConfiguration sslConfiguration,
  @NonNull ServiceInfoSnapshot serviceInfoSnapshot,
  @NonNull ServiceConfiguration serviceConfiguration) implements WrapperConfiguration {

  private static final Path WRAPPER_CONFIG_PATH = Path.of(
    System.getProperty("cloudnet.wrapper.config.path", ".wrapper/wrapper.json"));

  public static @NonNull WrapperConfiguration load() {
    return JsonDocument.newDocument(WRAPPER_CONFIG_PATH).toInstanceOf(DocumentWrapperConfiguration.class);
  }
}
