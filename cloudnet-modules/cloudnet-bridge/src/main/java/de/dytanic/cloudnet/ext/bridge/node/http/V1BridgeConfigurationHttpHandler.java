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

package de.dytanic.cloudnet.ext.bridge.node.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class V1BridgeConfigurationHttpHandler extends V1HttpHandler {

  public V1BridgeConfigurationHttpHandler(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "GET, POST");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    context
      .response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .header("Content-Type", "application/json")
      .body(GSON.toJson(CloudNetBridgeModule.getInstance().getBridgeConfiguration()))
      .context()
      .closeAfter(true)
      .cancelNext()
    ;
  }

  @Override
  public void handlePost(String path, IHttpContext context) throws Exception {
    try {
      if (context.request().body().length > 0) {
        BridgeConfiguration bridgeConfiguration = GSON
          .fromJson(context.request().bodyAsString(), BridgeConfiguration.TYPE);

        if (bridgeConfiguration != null) {
          CloudNetBridgeModule.getInstance().setBridgeConfiguration(bridgeConfiguration);
          CloudNetBridgeModule.getInstance().writeConfiguration(bridgeConfiguration);

          CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
            BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
            "update_bridge_configuration",
            new JsonDocument("bridgeConfiguration", bridgeConfiguration)
          );

          context
            .response()
            .statusCode(HttpResponseCode.HTTP_OK)
            .header("Content-Type", "application")
            .body(new JsonDocument("success", true).toByteArray())
            .context()
            .closeAfter(true)
            .cancelNext()
          ;
        }
      }

    } catch (Exception ex) {

      try (StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer)) {
        ex.printStackTrace(printWriter);
        this.send400Response(context, writer.getBuffer().toString());
      }
    }
  }
}
