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

package de.dytanic.cloudnet.ext.bridge.sponge.listener;

import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.SpongeExecutorService;

public final class SpongePlayerListener {

  private final SpongeExecutorService executorService;

  public SpongePlayerListener(SpongeCloudNetBridgePlugin plugin) {
    this.executorService = Sponge.getGame().getScheduler().createSyncExecutor(plugin);
  }

  @Listener
  public void handle(ClientConnectionEvent.Join event) {
    BridgeHelper
      .sendChannelMessageServerLoginSuccess(SpongeCloudNetHelper.createNetworkConnectionInfo(event.getTargetEntity()),
        SpongeCloudNetHelper.createNetworkPlayerServerInfo(event.getTargetEntity(), false)
      );

    this.executorService.execute(BridgeHelper::updateServiceInfo);
  }

  @Listener
  public void handle(ClientConnectionEvent.Disconnect event) {
    BridgeHelper
      .sendChannelMessageServerDisconnect(SpongeCloudNetHelper.createNetworkConnectionInfo(event.getTargetEntity()),
        SpongeCloudNetHelper.createNetworkPlayerServerInfo(event.getTargetEntity(), false));

    this.executorService.execute(BridgeHelper::updateServiceInfo);
  }
}
