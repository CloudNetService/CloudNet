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

package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.command;

import com.github.juliarn.npc.profile.Profile;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCAction;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloudNPCCommand implements CommandExecutor, TabCompleter {

  private static final Random RANDOM = new Random();

  private static final String DEFAULT_INFO_LINE = "§8• §7%online_players% of %max_players% players online §8•";

  private static final List<String> EDIT_COMMAND_PROPERTIES = Arrays.asList(
    "infoLine", "targetGroup", "skinOwnerName", "itemInHand", "shouldLookAtPlayer",
    "shouldImitatePlayer", "displayName", "rightClickAction", "leftClickAction"
  );

  private final BukkitNPCManagement npcManagement;

  public CloudNPCCommand(BukkitNPCManagement npcManagement) {
    this.npcManagement = npcManagement;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
    @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;

      if (args.length >= 7 && args[0].equalsIgnoreCase("create")) {
        this.createNPC(player, args);
      } else if (args.length >= 3 && args[0].equalsIgnoreCase("edit")) {
        this.editNPC(player, args);
      } else if (args.length == 1) {
        if (args[0].equalsIgnoreCase("remove")) {
          this.removeNPC(player);
        } else if (args[0].equalsIgnoreCase("list")) {
          this.listNPCs(player);
        } else if (args[0].equalsIgnoreCase("cleanup")) {
          this.cleanupNPCs(sender);
        } else {
          this.sendHelp(sender);
        }
      } else {
        this.sendHelp(sender);
      }

      return true;
    }

    return false;
  }

  private void createNPC(Player player, String[] args) {
    Material itemInHandMaterial = Material.getMaterial(args[3].toUpperCase());
    if (itemInHandMaterial == null) {
      player.sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-create-invalid-material"));
      return;
    }

    boolean lookAtPlayer = args[4].equalsIgnoreCase("true") || args[4].equalsIgnoreCase("yes");
    boolean imitatePlayer = args[5].equalsIgnoreCase("true") || args[5].equalsIgnoreCase("yes");

    Profile skinProfile = new Profile(args[2]);
    if (!skinProfile.complete()) {
      player
        .sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-create-texture-fetch-fail"));
      return;
    }

    StringBuilder displayNameBuilder = new StringBuilder();
    for (int i = 6; i < args.length; i++) {
      displayNameBuilder.append(args[i]).append(" ");
    }

    String displayName = displayNameBuilder.substring(0, displayNameBuilder.length() - 1);
    if (displayName.length() > 16) {
      player.sendMessage(
        this.npcManagement.getNPCConfiguration().getMessages().get("command-create-display-name-too-long"));
      return;
    }

    Location location = player.getLocation();
    CloudNPC cloudNPC = new CloudNPC(
      new UUID(RANDOM.nextLong(), 0),
      ChatColor.translateAlternateColorCodes('&', displayName),
      DEFAULT_INFO_LINE,
      skinProfile.getProperties().stream()
        .map(
          property -> new CloudNPC.NPCProfileProperty(property.getName(), property.getValue(), property.getSignature()))
        .collect(Collectors.toSet()),
      new WorldPosition(
        location.getX(),
        location.getY(),
        location.getZ(),
        location.getYaw(),
        location.getPitch(),
        location.getWorld().getName(),
        this.npcManagement.getOwnNPCConfigurationEntry().getTargetGroup()
      ),
      args[1],
      itemInHandMaterial.name(),
      lookAtPlayer,
      imitatePlayer
    );

    this.npcManagement.sendNPCAddUpdate(cloudNPC);
    player.sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-create-success"));
  }

  private void editNPC(Player player, String[] args) {
    this.getNearest(player).ifPresent(cloudNPC -> {
      StringBuilder valueBuilder = new StringBuilder();
      for (int i = 2; i < args.length; i++) {
        valueBuilder.append(args[i]).append(" ");
      }
      String value = ChatColor.translateAlternateColorCodes('&', valueBuilder.substring(0, valueBuilder.length() - 1));

      switch (args[1].toLowerCase()) {
        case "infoline": {
          cloudNPC.setInfoLine(value);
          break;
        }
        case "targetgroup": {
          cloudNPC.setTargetGroup(value.split(" ")[0]);
          break;
        }
        case "skinownername": {
          Profile skinProfile = new Profile(value.split(" ")[0]);
          if (!skinProfile.complete()) {
            player.sendMessage(
              this.npcManagement.getNPCConfiguration().getMessages().get("command-create-texture-fetch-fail"));
            return;
          }

          cloudNPC.setProfileProperties(skinProfile.getProperties().stream()
            .map(property -> new CloudNPC.NPCProfileProperty(property.getName(), property.getValue(),
              property.getSignature()))
            .collect(Collectors.toSet()));
          break;
        }
        case "iteminhand": {
          Material itemInHandMaterial = Material.getMaterial(value.toUpperCase());
          if (itemInHandMaterial == null) {
            player.sendMessage(
              this.npcManagement.getNPCConfiguration().getMessages().get("command-create-invalid-material"));
            return;
          }

          cloudNPC.setItemInHand(itemInHandMaterial.name());
          break;
        }
        case "shouldlookatplayer": {
          cloudNPC.setLookAtPlayer(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
          break;
        }
        case "shouldimitateplayer": {
          cloudNPC.setImitatePlayer(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
          break;
        }
        case "displayname": {
          if (value.length() > 16) {
            player.sendMessage(
              this.npcManagement.getNPCConfiguration().getMessages().get("command-create-display-name-too-long"));
            return;
          }

          cloudNPC.setDisplayName(value);
          break;
        }
        case "rightclickaction": {
          try {
            NPCAction action = NPCAction.valueOf(value.toUpperCase());
            cloudNPC.setRightClickAction(action);
          } catch (Exception exception) {
            player
              .sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-edit-invalid-action"));
            return;
          }

          break;
        }
        case "leftclickaction": {
          try {
            NPCAction action = NPCAction.valueOf(value.toUpperCase());
            cloudNPC.setLeftClickAction(action);
          } catch (Exception exception) {
            player
              .sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-edit-invalid-action"));
            return;
          }

          break;
        }
        default: {
          this.sendHelp(player);
          break;
        }
      }

      this.npcManagement.sendNPCAddUpdate(cloudNPC);
      player.sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-edit-success"));
    });
  }


  private void removeNPC(Player player) {
    this.getNearest(player).ifPresent(cloudNPC -> {
      this.npcManagement.sendNPCRemoveUpdate(cloudNPC);

      player.sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-remove-success"));
    });
  }

  private Optional<CloudNPC> getNearest(Player player) {
    Location location = player.getLocation();

    Optional<CloudNPC> optionalCloudNPC = this.npcManagement.getCloudNPCS().stream()
      .filter(cloudNPC -> {
        Location npcLocation = this.npcManagement.toLocation(cloudNPC.getPosition());

        return Objects.equals(npcLocation.getWorld(), player.getWorld()) && npcLocation.distance(location) <= 5D;
      })
      .min(Comparator
        .comparingDouble(cloudNPC -> this.npcManagement.toLocation(cloudNPC.getPosition()).distance(location)));

    if (!optionalCloudNPC.isPresent()) {
      player.sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-no-npc-in-range"));
    }

    return optionalCloudNPC;
  }

  private void listNPCs(Player player) {
    for (CloudNPC cloudNPC : this.npcManagement.getCloudNPCS()) {
      WorldPosition position = cloudNPC.getPosition();

      int x = (int) position.getX();
      int y = (int) position.getY();
      int z = (int) position.getZ();

      BaseComponent[] textComponent = new ComponentBuilder(String.format(
        "§8> %s §8- §7%d, %d, %d §8- §7%s",
        cloudNPC.getDisplayName(), x, y, z, position.getWorld()
      )).create();

      player.spigot().sendMessage(textComponent);
    }
  }

  private void cleanupNPCs(CommandSender sender) {
    this.npcManagement.getCloudNPCS().stream()
      .filter(npc -> !this.npcManagement.isWorldLoaded(npc))
      .forEach(this.npcManagement::sendNPCRemoveUpdate);

    sender.sendMessage(this.npcManagement.getNPCConfiguration().getMessages().get("command-cleanup-success"));
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(
      "§8> §7/cloudnpc create <targetGroup> <skinOwnerName> <itemInHand> <shouldLookAtPlayer> <shouldImitatePlayer> <displayName>");
    sender.sendMessage(String.format("§8> §7/cloudnpc edit <%s> <value>", String.join(", ", EDIT_COMMAND_PROPERTIES)));
    sender.sendMessage("§8> §7/cloudnpc remove");
    sender.sendMessage("§8> §7/cloudnpc list");
    sender.sendMessage("§8> §7/cloudnpc cleanup");
  }

  @Override
  @Nullable
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
    @NotNull String[] args) {
    if (args.length == 1) {
      return Arrays.asList("create", "edit", "remove", "list", "cleanup");
    }

    if (args[0].equalsIgnoreCase("create")) {
      if (args.length == 2) {
        return CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
          .map(GroupConfiguration::getName)
          .collect(Collectors.toList());
      } else if (args.length == 4) {
        return Arrays.stream(Material.values())
          .map(Material::name)
          .collect(Collectors.toList());
      } else if (args.length == 5 || args.length == 6) {
        return Arrays.asList("true", "false");
      }
    } else if (args[0].equalsIgnoreCase("edit")) {
      if (args.length == 2) {
        return EDIT_COMMAND_PROPERTIES;
      } else if (args.length == 3) {
        String editProperty = args[1];

        if (editProperty.equalsIgnoreCase("targetGroup")) {
          return CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
            .map(GroupConfiguration::getName)
            .collect(Collectors.toList());
        } else if (editProperty.equalsIgnoreCase("itemInHand")) {
          return Arrays.stream(Material.values())
            .map(Enum::name)
            .collect(Collectors.toList());
        } else if (editProperty.toLowerCase().startsWith("should")) {
          return Arrays.asList("true", "false");
        } else if (editProperty.toLowerCase().endsWith("action")) {
          return Arrays.stream(NPCAction.values())
            .map(Enum::name)
            .collect(Collectors.toList());
        }

      }
    }

    return Collections.emptyList();
  }

}
