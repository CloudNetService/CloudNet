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

package de.dytanic.cloudnet.examples.sign;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import de.dytanic.cloudnet.ext.signs.bukkit.event.BukkitCloudSignInteractEvent;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationTaskEntry;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

public final class ExampleSigns {

  // getting the SignManagement via CloudNet's service registry
  private final AbstractSignManagement signManagement = CloudNetDriver.getInstance().getServicesRegistry()
    .getFirstService(AbstractSignManagement.class);

  public void updateSigns() {
    this.signManagement.updateSigns();
  }

  public void foreachSigns() {
    for (Sign sign : this.signManagement.getSigns()) {
      // ...
    }
  }

  public void customizeSignLayout() {
    this.signManagement.getOwnSignConfigurationEntry().getTaskLayouts().add(new SignConfigurationTaskEntry(
      "Lobby",
      new SignLayout(
        new String[]{
          "Lobby-1",
          "%online_count% / %max_players%",
          "A minecraft server",
          "LOBBY"
        },
        Material.STONE.name(),
        0
      ),
      new SignLayout(
        new String[]{
          "Lobby-1",
          "%online_count% / %max_players%",
          "A minecraft server",
          "LOBBY"
        },
        Material.STONE.name(),
        0
      ),
      new SignLayout(
        new String[]{
          "Lobby-1",
          "%online_count% / %max_players%",
          "A minecraft server",
          "LOBBY"
        },
        Material.STONE.name(),
        0
      )
    ));
  }

  @EventHandler
  public void handleSignInteract(BukkitCloudSignInteractEvent event) {
    Sign sign = event.getClickedSign();

    event.getPlayer().sendMessage(String.format("You clicked on a sign targeting group %s!", sign.getTargetGroup()));

    // sending the Player to any desired service
    event.setTargetServer("PeepoHub-2");

    // cancelling the event, the Player won't be sent to any service
    event.setCancelled(true);
  }

}
