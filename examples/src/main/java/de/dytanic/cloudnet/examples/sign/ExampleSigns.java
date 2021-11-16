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

import com.google.common.base.Strings;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.bukkit.event.BukkitCloudSignInteractEvent;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry.KnockbackConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

public final class ExampleSigns {

  // getting the SignManagement via CloudNet's service registry
  private final SignManagement signManagement = CloudNetDriver.getInstance().getServicesRegistry()
    .getFirstService(SignManagement.class);

  private static @NotNull SignLayout createLayout(String firstLine, String block, int amount) {
    return new SignLayout(
      new String[]{
        "",
        firstLine,
        Strings.repeat(".", amount),
        ""
      }, block, -1
    );
  }

  public void foreachSigns() {
    for (Sign sign : this.signManagement.getSigns()) {
      // ...
    }
  }

  public void customizeSignLayout() {
    String block = Material.STONE.name();

    this.signManagement.getSignsConfiguration().getConfigurationEntries().add(new SignConfigurationEntry(
      "Lobby", // the group the sign is located on
      false,
      new KnockbackConfiguration(1, 0.8),
      new ArrayList<>(Collections.singleton(new SignGroupConfiguration(
        "Target_Group",
        new SignLayoutsHolder(1,
          new ArrayList<>(Collections.singleton(new eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout(
            new String[]{
              "&7Lobby &0- &7%task_id%",
              "&8[&7LOBBY&8]",
              "%online_players% / %max_players%",
              "%motd%"
            }, Material.STONE.name(), -1)
          ))), new SignLayoutsHolder(1,
        new ArrayList<>(Collections.singleton(new eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout(
          new String[]{
            "&eLobby &0- &e%task_id%",
            "&8[&eLOBBY&8]",
            "%online_players% / %max_players%",
            "%motd%"
          }, Material.STONE.name(), -1)
        ))), new SignLayoutsHolder(1,
        new ArrayList<>(Collections.singleton(new eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout(
          new String[]{
            "&6Lobby &0- &6%task_id%",
            "&8[&6PRIME&8]",
            "%online_players% / %max_players%",
            "%motd%"
          }, Material.STONE.name(), -1)
        )))))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("Waiting", block, 1),
        createLayout("Waiting", block, 1),
        createLayout("Waiting", block, 2),
        createLayout("Waiting", block, 2),
        createLayout("Waiting", block, 3),
        createLayout("Waiting", block, 3)
      ))
    ), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("Starting", block, 1),
        createLayout("Starting", block, 1),
        createLayout("Starting", block, 2),
        createLayout("Starting", block, 2),
        createLayout("Starting", block, 3),
        createLayout("Starting", block, 3)
      ))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("&8[&7LOBBY&8]", block, 1),
        createLayout("&8[&7LOBBY&8]", block, 1),
        createLayout("&8[&7LOBBY&8]", block, 2),
        createLayout("&8[&7LOBBY&8]", block, 2),
        createLayout("&8[&7LOBBY&8]", block, 3),
        createLayout("&8[&7LOBBY&8]", block, 3)
      ))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("&8[&eLOBBY&8]", block, 1),
        createLayout("&8[&eLOBBY&8]", block, 1),
        createLayout("&8[&eLOBBY&8]", block, 2),
        createLayout("&8[&eLOBBY&8]", block, 2),
        createLayout("&8[&eLOBBY&8]", block, 3),
        createLayout("&8[&eLOBBY&8]", block, 3)
      ))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("&8[&6&lLOBBY&8]", block, 1),
        createLayout("&8[&6&lLOBBY&8]", block, 1),
        createLayout("&8[&6&lLOBBY&8]", block, 2),
        createLayout("&8[&6&lLOBBY&8]", block, 2),
        createLayout("&8[&6&lLOBBY&8]", block, 3),
        createLayout("&8[&6&lLOBBY&8]", block, 3)
      )))
    ));
  }

  @EventHandler
  public void handleSignInteract(BukkitCloudSignInteractEvent event) {
    Sign sign = event.getClickedSign();

    event.getPlayer().sendMessage(String.format("You clicked on a sign targeting group %s!", sign.getTargetGroup()));

    // sending the Player to any desired service
    event.setTarget(CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServiceByName("PeepoHub-2"));

    // cancelling the event, the Player won't be sent to any service
    event.setCancelled(true);
  }

}
