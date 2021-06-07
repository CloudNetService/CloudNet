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

package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class BridgeConfigurationProvider {

  private static BridgeConfiguration loadedConfiguration;

  private BridgeConfigurationProvider() {
    throw new UnsupportedOperationException();
  }

  public static BridgeConfiguration update(BridgeConfiguration bridgeConfiguration) {
    Preconditions.checkNotNull(bridgeConfiguration);

    BridgeHelper.messageBuilder()
      .message(BridgeConstants.BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER)
      .json(JsonDocument.newDocument("bridgeConfiguration", bridgeConfiguration))
      .targetAll()
      .build()
      .send();
    loadedConfiguration = bridgeConfiguration;

    return bridgeConfiguration;
  }

  public static void setLocal(BridgeConfiguration bridgeConfiguration) {
    Preconditions.checkNotNull(bridgeConfiguration);

    loadedConfiguration = bridgeConfiguration;
  }

  public static BridgeConfiguration load() {
    if (loadedConfiguration == null) {
      loadedConfiguration = load0();
    }

    return loadedConfiguration;
  }

  private static BridgeConfiguration load0() {
    ChannelMessage response = BridgeHelper.messageBuilder()
      .message(BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    return response != null ? response.getJson().get("bridgeConfig", BridgeConfiguration.TYPE) : null;
  }
}
