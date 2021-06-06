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

package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.util.stream.Collectors;

public final class V1HttpHandlerStatus extends V1HttpHandler {

  public V1HttpHandlerStatus(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "OPTIONS, GET");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    IConfiguration configuration = this.getCloudNet().getConfig();

    context
      .response()
      .header("Content-Type", "application/json")
      .body(
        new JsonDocument()
          .append("Version", V1HttpHandlerStatus.class.getPackage().getImplementationVersion())
          .append("Version-Title", V1HttpHandlerStatus.class.getPackage().getImplementationTitle())
          .append("Identity", configuration.getIdentity())
          .append("currentNetworkClusterNodeInfoSnapshot",
            this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot())
          .append("lastNetworkClusterNodeInfoSnapshot", this.getCloudNet().getLastNetworkClusterNodeInfoSnapshot())
          .append("providedServicesCount", this.getCloudNet().getCloudServiceProvider().getServicesCount())
          .append("modules", super.getCloudNet().getModuleProvider().getModules().stream()
            .map(moduleWrapper -> moduleWrapper.getModuleConfiguration().getGroup() + ":" +
              moduleWrapper.getModuleConfiguration().getName() + ":" +
              moduleWrapper.getModuleConfiguration().getVersion())
            .collect(Collectors.toList()))
          .append("clientConnections", super.getCloudNet().getNetworkClient().getChannels().stream()
            .map(INetworkChannel::getServerAddress)
            .collect(Collectors.toList()))
          .toByteArray()
      )
      .statusCode(200)
      .context()
      .closeAfter(true)
      .cancelNext()
    ;
  }
}
