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

package de.dytanic.cloudnet.ext.signs.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;
import java.util.Collection;

public final class CloudNetSignsModuleListener {

  @EventListener
  public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
    event.getNode().sendCustomChannelMessage(
      SignConstants.SIGN_CLUSTER_CHANNEL_NAME,
      SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
      new JsonDocument()
        .append("signConfiguration", CloudNetSignsModule.getInstance().getSignConfiguration())
        .append("signs", CloudNetSignsModule.getInstance().loadSigns())
    );
  }

  @EventListener
  public void handleQuery(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(SignConstants.SIGN_CHANNEL_NAME) || event.getMessage() == null || !event
      .isQuery()) {
      return;
    }

    switch (event.getMessage().toLowerCase()) {
      case SignConstants.SIGN_CHANNEL_GET_SIGNS: {
        event.setJsonResponse(JsonDocument.newDocument("signs", CloudNetSignsModule.getInstance().loadSigns()));
      }
      break;
      case SignConstants.SIGN_CHANNEL_GET_SIGNS_CONFIGURATION: {
        event.setJsonResponse(
          JsonDocument.newDocument("signConfiguration", CloudNetSignsModule.getInstance().getSignConfiguration()));
      }
      break;
      default:
        break;
    }
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (event.getMessage() == null) {
      return;
    }

    if (event.getChannel().equalsIgnoreCase(SignConstants.SIGN_CLUSTER_CHANNEL_NAME)) {
      if (SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION.equalsIgnoreCase(event.getMessage())) {
        SignConfiguration signConfiguration = event.getData().get("signConfiguration", SignConfiguration.TYPE);
        Collection<Sign> signs = event.getData().get("signs", SignConstants.COLLECTION_SIGNS);

        CloudNetSignsModule.getInstance().setSignConfiguration(signConfiguration);
        SignConfigurationReaderAndWriter
          .write(signConfiguration, CloudNetSignsModule.getInstance().getConfigurationFilePath());

        CloudNetSignsModule.getInstance().write(signs);
      }
    }

    if (event.getChannel().equals(SignConstants.SIGN_CHANNEL_NAME)) {
      switch (event.getMessage().toLowerCase()) {
        case SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE: {
          Sign sign = event.getData().get("sign", Sign.TYPE);

          if (sign != null) {
            CloudNetSignsModule.getInstance().addSignToFile(sign);
          }
        }
        break;
        case SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE: {
          Sign sign = event.getData().get("sign", Sign.TYPE);

          if (sign != null) {
            CloudNetSignsModule.getInstance().removeSignToFile(sign);
          }
        }
        break;
        default:
          break;
      }
    }
  }
}
