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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;

/**
 * The default json based wrapper configuration for the service. It loads only the configuration with the constructor
 * all properties once.
 *
 * @see IWrapperConfiguration
 */
public final class DocumentWrapperConfiguration implements IWrapperConfiguration {

  private static final Path WRAPPER_CONFIG_PATH = Paths.get(
    System.getProperty("cloudnet.wrapper.config.path", ".wrapper/wrapper.json"));

  private final String connectionKey;
  private final HostAndPort targetListener;
  private final SSLConfiguration sslConfiguration;
  private final ServiceInfoSnapshot serviceInfoSnapshot;
  private final ServiceConfiguration serviceConfiguration;

  public DocumentWrapperConfiguration(
    @NotNull String connectionKey,
    @NotNull HostAndPort targetListener,
    @NotNull SSLConfiguration sslConfiguration,
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot,
    @NotNull ServiceConfiguration serviceConfiguration
  ) {
    this.connectionKey = connectionKey;
    this.targetListener = targetListener;
    this.sslConfiguration = sslConfiguration;
    this.serviceInfoSnapshot = serviceInfoSnapshot;
    this.serviceConfiguration = serviceConfiguration;
  }

  public static @NotNull IWrapperConfiguration load() {
    return JsonDocument.newDocument(WRAPPER_CONFIG_PATH).toInstanceOf(DocumentWrapperConfiguration.class);
  }

  @Override
  public @NotNull String getConnectionKey() {
    return this.connectionKey;
  }

  @Override
  public @NotNull HostAndPort getTargetListener() {
    return this.targetListener;
  }

  @Override
  public @NotNull ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }

  @Override
  public @NotNull ServiceConfiguration getServiceConfiguration() {
    return this.serviceConfiguration;
  }

  @Override
  public @NotNull SSLConfiguration getSSLConfig() {
    return this.sslConfiguration;
  }
}
