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

package de.dytanic.cloudnet.wrapper.conf;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The default json based wrapper configuration for the service. It loads only the configuration with the constructor
 * all properties once.
 *
 * @see IWrapperConfiguration
 */
public final class DocumentWrapperConfiguration implements IWrapperConfiguration {

  private static final Path WRAPPER_CONFIG = Paths
    .get(System.getProperty("cloudnet.wrapper.config.path", ".wrapper/wrapper.json"));

  private static final Type SERVICE_CFG_TYPE = ServiceConfiguration.class;
  private static final Type SERVICE_INFO_TYPE = ServiceInfoSnapshot.class;

  private String connectionKey;

  private HostAndPort targetListener;

  private ServiceInfoSnapshot serviceInfoSnapshot;

  private ServiceConfiguration serviceConfiguration;

  private JsonDocument sslConfig;

  public DocumentWrapperConfiguration() {
    this.load();
  }

  private void load() {
    JsonDocument document = JsonDocument.newDocument(WRAPPER_CONFIG);

    this.connectionKey = document.getString("connectionKey");
    this.targetListener = document.get("listener", HostAndPort.class);
    this.serviceConfiguration = document.get("serviceConfiguration", SERVICE_CFG_TYPE);
    this.serviceInfoSnapshot = document.get("serviceInfoSnapshot", SERVICE_INFO_TYPE);
    this.sslConfig = document.getDocument("sslConfig");
  }

  public String getConnectionKey() {
    return this.connectionKey;
  }

  public HostAndPort getTargetListener() {
    return this.targetListener;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }

  public ServiceConfiguration getServiceConfiguration() {
    return this.serviceConfiguration;
  }

  public JsonDocument getSslConfig() {
    return this.sslConfig;
  }
}
