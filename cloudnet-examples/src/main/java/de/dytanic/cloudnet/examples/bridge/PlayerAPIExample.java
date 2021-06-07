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

package de.dytanic.cloudnet.examples.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class PlayerAPIExample {

  // getting the PlayerManager via CloudNet's ServicesRegistry
  private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry()
    .getFirstService(IPlayerManager.class);

  //Returns the player online count from a task synchronized
  public int countServiceInfoSnapshotPlayerCount() {
    int counter = 0;

    for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider()
      .getCloudServices("Lobby")) {
      counter += serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0);
    }

    return counter;
  }

  //Asynchronous variant of count the player count from a task
  public void countServiceInfoSnapshotPlayerCount(Consumer<Integer> consumer) {
    CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync("Lobby")
      .onComplete(serviceInfoSnapshots -> {
        int counter = 0;

        if (serviceInfoSnapshots != null) {
          for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider()
            .getCloudServices("Lobby")) {
            counter += serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0);
          }
        }

        consumer.accept(counter);
      });
  }

  public void getPlayerExample(Player player) { // Bukkit player
    Preconditions.checkNotNull(player);

    ICloudPlayer cloudPlayer = this.playerManager
      .getOnlinePlayer(player.getUniqueId()); //Returns an online cloudPlayers

    // Checks if the player is online
    if (cloudPlayer != null) {
      cloudPlayer.getUniqueId(); //Returns the uniqueId
      cloudPlayer.getName(); //Returns the players name
      cloudPlayer.getXBoxId(); //Bedrock Edition XBoxId
    }

    ICloudOfflinePlayer cloudOfflinePlayer = this.playerManager.getOfflinePlayer(player.getUniqueId());
    // Returns the cloud offline player with all players data from the database

    // Checks the cloud offline player if it registered on CloudNet
    if (cloudOfflinePlayer != null) {
      cloudOfflinePlayer.getFirstLoginTimeMillis(); //First login in milliseconds since 1.1.1970
      cloudOfflinePlayer.getLastLoginTimeMillis(); //Last login or the current login in milliseconds
    }

    List<? extends ICloudPlayer> cloudPlayers = this.playerManager.getOnlinePlayers(player.getName());
    if (!cloudPlayers.isEmpty()) {
      ICloudPlayer entry = cloudPlayers.get(0);

      this.playerManager.getPlayerExecutor(entry).kick("Kick message"); //kicks a player from the network
      this.playerManager.getPlayerExecutor(entry)
        .connect("Lobby-3"); //send a player to the target server if the player is login on a proxy
      this.playerManager.getPlayerExecutor(entry).sendChatMessage("Hello, player!"); //send the player a text message
    }
  }

  //Add additional properties
  public void addPropertiesToPlayer(Player player) { // Bukkit org.bukkit.entity.Player
    ICloudPlayer cloudPlayer = this.playerManager.getOnlinePlayer(player.getUniqueId());

    if (cloudPlayer != null) {
      cloudPlayer.getProperties().append("my custom property", 42);
      this.playerManager.updateOnlinePlayer(cloudPlayer);
    }
  }

  //Handles an online player update only
  @EventListener
  public void handle(BridgeUpdateCloudPlayerEvent event) {
    ICloudPlayer cloudPlayer = event.getCloudPlayer();

    int myCustomProperty = cloudPlayer.getProperties().getInt("my custom property"); //42
  }

  //Handles an offline player update only
  @EventListener
  public void handle(BridgeUpdateCloudOfflinePlayerEvent event) {
    ICloudOfflinePlayer cloudPlayer = event.getCloudOfflinePlayer();

    /* ... */ //do something
  }
}
