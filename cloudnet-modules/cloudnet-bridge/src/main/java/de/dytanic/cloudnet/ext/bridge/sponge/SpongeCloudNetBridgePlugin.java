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

package de.dytanic.cloudnet.ext.bridge.sponge;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.listener.SpongeCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.sponge.listener.SpongePlayerListener;
import de.dytanic.cloudnet.ext.bridge.sponge.platform.CloudNetSpongeClientPingEventResponse;
import de.dytanic.cloudnet.ext.bridge.sponge.platform.CloudNetSpongeStatusClient;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.concurrent.TimeUnit;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.StatusResponse;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;

@Plugin(
  id = "cloudnet_bridge",
  name = "CloudNet-Bridge",
  version = "1.0",
  description = "Sponge extension for the CloudNet runtime, which optimize some features",
  url = "https://cloudnetservice.eu"
)
public final class SpongeCloudNetBridgePlugin {

  private SpongeExecutorService executorService;

  @Listener
  public synchronized void handle(GameStartedServerEvent event) {
    SpongeCloudNetHelper.init();

    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(IPlayerManager.class, "BridgePlayerManager", new BridgePlayerManager());

    Sponge.getChannelRegistrar().createChannel(this, "bungeecord:main");
    Sponge.getChannelRegistrar().createChannel(this, "cloudnet:main");

    this.initListeners();
    BridgeHelper.updateServiceInfo();

    this.executorService = Sponge.getScheduler().createAsyncExecutor(this);
    this.runFireServerListPingEvent();
  }

  @Listener
  public synchronized void handle(GameStoppingServerEvent event) {
    Sponge.getEventManager().unregisterListeners(this);
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    this.executorService.shutdownNow();
  }

  private void initListeners() {
    //Sponge API
    Sponge.getEventManager().registerListeners(this, new SpongePlayerListener(this));

    //CloudNet
    CloudNetDriver.getInstance().getEventManager().registerListener(new SpongeCloudNetListener());
    CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
  }

  private void runFireServerListPingEvent() {
    this.executorService.scheduleAtFixedRate(() -> {
      ClientPingServerEvent clientPingServerEvent = SpongeEventFactory.createClientPingServerEvent(
        Cause.of(EventContext.empty(), Sponge.getServer()),
        CloudNetSpongeStatusClient.INSTANCE,
        new CloudNetSpongeClientPingEventResponse()
      );
      Sponge.getEventManager().post(clientPingServerEvent);

      if (clientPingServerEvent.isCancelled()) {
        return;
      }

      boolean hasToUpdate = false;
      boolean value = false;

      String plainDescription = clientPingServerEvent.getResponse().getDescription().toPlain();
      if (!plainDescription.equalsIgnoreCase(BridgeServerHelper.getMotd())) {
        hasToUpdate = true;
        BridgeServerHelper.setMotd(plainDescription);

        String lowerMotd = plainDescription.toLowerCase();
        if (lowerMotd.contains("running") || lowerMotd.contains("ingame") || lowerMotd.contains("playing")) {
          value = true;
        }
      }

      int max = clientPingServerEvent.getResponse().getPlayers().map(StatusResponse.Players::getMax).orElse(-1);
      if (max >= 0 && max != BridgeServerHelper.getMaxPlayers()) {
        hasToUpdate = true;
        BridgeServerHelper.setMaxPlayers(max);
      }

      if (value) {
        BridgeServerHelper.changeToIngame(true);
        return;
      }

      if (hasToUpdate) {
        BridgeHelper.updateServiceInfo();
      }
    }, 500, 500, TimeUnit.MILLISECONDS);
  }
}
