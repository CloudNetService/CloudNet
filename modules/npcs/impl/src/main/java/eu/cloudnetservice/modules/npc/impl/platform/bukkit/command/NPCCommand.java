/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit.command;

import com.github.juliarn.npclib.api.profile.Profile;
import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.ext.bukkitcommands.BaseTabExecutor;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.utils.base.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class NPCCommand extends BaseTabExecutor {

  private static final List<String> MATERIAL_NAMES = Arrays.stream(Material.values()).map(Material::name).toList();

  private static final List<String> TRUE_FALSE = Arrays.asList("true", "yes", "y", "false", "no", "n");

  private static final List<String> NPC_TYPES = Arrays.stream(NPC.NPCType.values())
    .map(Enum::name)
    .toList();
  private static final List<String> CLICK_ACTIONS = Arrays.stream(NPC.ClickAction.values())
    .map(Enum::name)
    .toList();
  private static final Map<String, Integer> VALID_ITEM_SLOTS = ImmutableMap.<String, Integer>builder()
    .put("MAIN_HAND", 0)
    .put("OFF_HAND", 1)
    .put("BOOTS", 2)
    .put("LEGGINS", 3)
    .put("CHESTPLATE", 4)
    .put("HELMET", 5)
    .build();

  private static final String COPIED_NPC_KEY = "npc_copy_entry";

  private final Plugin plugin;
  private final Server server;
  private final BukkitPlatformNPCManagement management;
  private final GroupConfigurationProvider groupConfigurationProvider;

  @Inject
  public NPCCommand(
    @NonNull Plugin plugin,
    @NonNull Server server,
    @NonNull BukkitPlatformNPCManagement management,
    @NonNull GroupConfigurationProvider groupConfigurationProvider
  ) {
    this.plugin = plugin;
    this.server = server;
    this.management = management;
    this.groupConfigurationProvider = groupConfigurationProvider;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // validate that the sender is a player
    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cOnly players can execute the /cn command!");
      return true;
    }

    // get the npc configuration entry for the current group
    var entry = this.management.applicableNPCConfigurationEntry();
    if (entry == null) {
      sender.sendMessage("§cThere is no applicable npc configuration entry for this service (yet)!");
      return true;
    }

    // npc create
    if (args.length >= 4 && args[0].equalsIgnoreCase("create")) {
      // 0: target group
      var targetGroup = args[1];
      // 1: mob type
      var npcType = Enums.getIfPresent(NPC.NPCType.class, StringUtil.toUpper(args[2])).orNull();
      if (npcType == null) {
        sender.sendMessage("§cNo such NPC type, use one of: " + String.join(", ", NPC_TYPES));
        return true;
      }
      // 2: skin owner or entity type, depends on 1
      if (npcType == NPC.NPCType.PLAYER) {
        // load the profile
        this.management.npcPlatform().profileResolver().resolveProfile(Profile.unresolved(args[3]))
          .thenAccept(profile -> {
            // create the npc
            var npc = NPC.builder()
              .profileProperties(profile.properties().stream()
                .map(prop -> new NPC.ProfileProperty(prop.name(), prop.value(), prop.signature()))
                .collect(Collectors.toSet()))
              .targetGroup(targetGroup)
              .location(this.management.toWorldPosition(player.getLocation(), entry.targetGroup()))
              .build();
            this.management.createNPC(npc);
          }).exceptionally(throwable -> {
            sender.sendMessage(String.format("§cUnable to complete profile of §6%s§c!", args[3]));
            return null;
          });
      } else {
        // get the entity type
        var entityType = Enums.getIfPresent(EntityType.class, StringUtil.toUpper(args[3])).orNull();
        if (entityType == null
          || !entityType.isSpawnable()
          || !entityType.isAlive()
          || entityType == EntityType.PLAYER
        ) {
          sender.sendMessage(String.format("§cYou can not spawn a selector mob of type §6%s§c!", args[3]));
          return true;
        }
        // create the npc
        var npc = NPC.builder()
          .entityType(entityType.name())
          .targetGroup(targetGroup)
          .location(this.management.toWorldPosition(player.getLocation(), entry.targetGroup()))
          .build();
        this.management.createNPC(npc);
      }

      // done :)
      sender.sendMessage("§7The service selector mob was created §asuccessfully§7!");
      return true;
    }

    // npc remove all
    if (args.length >= 1 && args[0].equalsIgnoreCase("removeall")) {
      int removedNpcs;

      if (args.length == 2) {
        // 1: optional target group
        removedNpcs = this.management.deleteAllNPCs(args[1]);
      } else {
        removedNpcs = this.management.deleteAllNPCs();
      }

      sender.sendMessage(String.format("§7Successfully removed %d npcs.", removedNpcs));
      return true;
    }

    // npc operations
    if (args.length == 1) {
      switch (StringUtil.toLower(args[0])) {
        // remove the nearest npc
        case "rm", "remove" -> {
          var npc = this.getNearestNPC(player.getLocation());
          if (npc == null) {
            sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
            return true;
          }
          // remove the npc
          this.management.deleteNPC(npc);
          sender.sendMessage("§cThe npc was removed successfully! This may take a few seconds to show effect!");
          return true;
        }

        // removes all npcs in unloaded worlds
        case "cu", "cleanup" -> {
          this.management.trackedEntities().values().stream()
            .map(PlatformSelectorEntity::npc)
            .filter(npc -> this.server.getWorld(npc.location().world()) == null)
            .forEach(npc -> {
              this.management.deleteNPC(npc);
              sender.sendMessage(String.format(
                "§cAn entity in the world §6%s §cwas removed! This may take a few seconds to show effect!",
                npc.location().world()));
            });
          return true;
        }

        // copies the current npc
        case "cp", "copy" -> {
          var npc = this.getNearestNPC(player.getLocation());
          if (npc == null) {
            sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
            return true;
          }
          // check if the player has already a npc in the clipboard
          if (player.getMetadata(COPIED_NPC_KEY).isEmpty()) {
            // add the metadata
            player.setMetadata(COPIED_NPC_KEY, new FixedMetadataValue(this.plugin, NPC.builder(npc)));
            sender.sendMessage("§7The npc was copied §asuccessfully §7to your clipboard");
          } else {
            sender.sendMessage("§cThere is a npc already in your clipboard! Paste it or clear your clipboard.");
          }
          return true;
        }

        // clears the clipboard
        case "ccb", "clearclipboard" -> {
          player.removeMetadata(COPIED_NPC_KEY, this.plugin);
          sender.sendMessage("§7Your clipboard was cleared §asuccessfully§7.");
          return true;
        }

        // cuts the npc
        case "cut" -> {
          var npc = this.getNearestNPC(player.getLocation());
          if (npc == null) {
            sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
            return true;
          }
          // check if the player has already a npc in the clipboard
          if (player.getMetadata(COPIED_NPC_KEY).isEmpty()) {
            // remove the npc
            this.management.deleteNPC(npc);
            // add the metadata
            player.setMetadata(COPIED_NPC_KEY, new FixedMetadataValue(this.plugin, NPC.builder(npc)));
            sender.sendMessage("§7The npc was cut §asuccessfully §7to your clipboard");
          } else {
            sender.sendMessage("§cThere is a npc already in your clipboard! Paste it or clear your clipboard.");
          }
          return true;
        }

        // pastes the npc
        case "paste" -> {
          var values = player.getMetadata(COPIED_NPC_KEY);
          if (values.isEmpty()) {
            sender.sendMessage("§cThere is no npc in your clipboard!");
            return true;
          }
          // paste the npc
          var npc = ((NPC.Builder) values.get(0).value())
            .location(this.management.toWorldPosition(player.getLocation(), entry.targetGroup()))
            .build();
          this.management.createNPC(npc);
          sender.sendMessage("§7The service selector mob was pasted §asuccessfully§7!");
          // clear the clipboard
          player.removeMetadata(COPIED_NPC_KEY, this.plugin);
          return true;
        }

        // lists all npcs
        case "list" -> {
          sender.sendMessage(String.format("§7There are §6%s §7selector mobs:", this.management.npcs().size()));
          for (var npc : this.management.npcs()) {
            sender.sendMessage(String.format(
              "§8> §7%s§8/§7%s §8- §7%d, %d, %d in \"%s\"",
              npc.npcType(),
              npc.npcType() == NPC.NPCType.ENTITY ? npc.entityType() : "props: " + npc.profileProperties().size(),
              (int) npc.location().x(),
              (int) npc.location().y(),
              (int) npc.location().z(),
              npc.location().world()));
          }
          return true;
        }
        default -> {
          sender.sendMessage(String.format("§cUnknown sub-command option: §6%s§c.", args[0]));
          return true;
        }
      }
    }

    // npc edit
    if (args.length >= 3 && args[0].equalsIgnoreCase("edit")) {
      final var npc = this.getNearestNPC(player.getLocation());
      if (npc == null) {
        sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
        return true;
      }
      NPC updatedNpc;
      // find the option the player is trying to edit
      switch (StringUtil.toLower(args[1])) {
        // enable that the npc looks at the player
        case "lap", "lookatplayer" -> {
          if (this.canChangeSetting(sender, npc)) {
            updatedNpc = NPC.builder(npc).lookAtPlayer(this.parseBoolean(args[2])).build();
          } else {
            return true;
          }
        }

        // if the npc should imitate the player
        case "ip", "imitateplayer" -> {
          if (this.canChangeSetting(sender, npc)) {
            updatedNpc = NPC.builder(npc).imitatePlayer(this.parseBoolean(args[2])).build();
          } else {
            return true;
          }
        }

        // if the npc should use the skin of the player being spawned to
        case "ups", "useplayerskin" -> {
          if (this.canChangeSetting(sender, npc)) {
            updatedNpc = NPC.builder(npc).usePlayerSkin(this.parseBoolean(args[2])).build();
          } else {
            return true;
          }
        }

        // if the npc should show ingame services in the inventory
        case "sis", "showingameservices" ->
          updatedNpc = NPC.builder(npc).showIngameServices(this.parseBoolean(args[2])).build();

        // if the npc should full services in the inventory
        case "sfs", "showfullservices" ->
          updatedNpc = NPC.builder(npc).showFullServices(this.parseBoolean(args[2])).build();

        // sets the glowing color
        case "gc", "glowingcolor" -> {
          // try to parse the color
          var chatColor = ChatColor.getByChar(args[2]);
          if (chatColor == null) {
            sender.sendMessage(String.format(
              "§cNo such chat color char §6%s§c! Use one of §8[§60-9§8, §6a-f§8, §6r§8]§c.",
              args[2]));
            return true;
          }
          // validate the color
          if (chatColor.isFormat()) {
            sender.sendMessage("§cPlease use a color char, not a chat formatting char!");
            return true;
          }
          // disable glowing if the color is reset
          if (chatColor == ChatColor.RESET) {
            updatedNpc = NPC.builder(npc).glowing(false).build();
          } else {
            updatedNpc = NPC.builder(npc).glowing(true).glowingColor(String.valueOf(chatColor.getChar())).build();
          }
        }

        // sets if the npc should "fly" with an elytra
        case "fwe", "flyingwithelytra" -> {
          if (this.canChangeSetting(sender, npc)) {
            var enabled = this.parseBoolean(args[2]);
            updatedNpc = NPC.builder(npc).flyingWithElytra(enabled).build();
            // warn about weird behaviour in combination with other settings
            if (enabled) {
              sender.sendMessage("§cEnabling elytra-flying might lead to weird-looking behaviour when imitate "
                + "and lookAt player is enabled! Consider disabling these options.");
            }
          } else {
            return true;
          }
        }

        // sets if the npc should burn
        case "burning" -> {
          if (this.canChangeSetting(sender, npc)) {
            var enabled = this.parseBoolean(args[2]);
            updatedNpc = NPC.builder(npc).burning(enabled).build();
          } else {
            return true;
          }
        }

        // the floating item of the npc
        case "fi", "floatingitem" -> {
          // convert null to "no item"
          if (args[2].equalsIgnoreCase("null")) {
            updatedNpc = NPC.builder(npc).floatingItem(null).build();
            break;
          }
          // get the material of the item
          var material = Material.matchMaterial(args[2]);
          if (material == null) {
            sender.sendMessage(String.format("§cNo material found by query: §6%s§c.", args[2]));
            return true;
          } else if (material == Material.AIR) {
            updatedNpc = NPC.builder(npc).floatingItem(null).build();
          } else {
            updatedNpc = NPC.builder(npc).floatingItem(material.name()).build();
          }
        }

        // the left click action
        case "lca", "leftclickaction" -> {
          var action = Enums.getIfPresent(NPC.ClickAction.class, StringUtil.toUpper(args[2])).orNull();
          if (action == null) {
            sender.sendMessage(String.format(
              "§cNo such click action. Use one of: §6%s§c.",
              String.join(", ", CLICK_ACTIONS)));
            return true;
          } else {
            updatedNpc = NPC.builder(npc).leftClickAction(action).build();
          }
        }

        // the right click action
        case "rca", "rightclickaction" -> {
          var action = Enums.getIfPresent(NPC.ClickAction.class, StringUtil.toUpper(args[2])).orNull();
          if (action == null) {
            sender.sendMessage(String.format(
              "§cNo such click action. Use one of: §6%s§c.",
              String.join(", ", CLICK_ACTIONS)));
            return true;
          } else {
            updatedNpc = NPC.builder(npc).rightClickAction(action).build();
          }
        }

        // sets the items
        case "items" -> {
          if (args.length != 4) {
            sender.sendMessage("§cInvalid usage! Use §6/cn edit items <slot> <material>§c!");
            return true;
          }
          // parse the slot
          var slot = VALID_ITEM_SLOTS.get(StringUtil.toUpper(args[2]));
          if (slot == null) {
            sender.sendMessage(String.format(
              "§cNo such item slot! Use one of §6%s§7.",
              String.join(", ", VALID_ITEM_SLOTS.keySet())));
            return true;
          }
          // parse the item
          var item = Material.matchMaterial(args[3]);
          if (item == null) {
            sender.sendMessage("§cNo such material!");
            return true;
          }
          // a little hack here :)
          npc.items().put(slot, item.name());
          updatedNpc = npc;
        }

        // edit the info lines
        case "il", "infolines" -> {
          if (args.length < 4) {
            sender.sendMessage("§cInvalid usage! Use §6/cn edit il <index> <new line content>§c!");
            return true;
          }
          // parse the index
          var index = Ints.tryParse(args[2]);
          if (index == null) {
            sender.sendMessage(String.format("§cUnable to parse index from string §6%s§c.", args[2]));
            return true;
          }
          // get the new line content
          var content = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
          if (content.equals("null")) {
            // remove the info line if there
            if (npc.infoLines().size() > index) {
              npc.infoLines().remove((int) index);
              updatedNpc = npc;
            } else {
              sender.sendMessage(String.format("§cNo info line at index §6%d§c.", index));
              return true;
            }
          } else {
            content = ChatColor.translateAlternateColorCodes('&', content);
            // set the info line add the location or add it
            if (npc.infoLines().size() > index) {
              npc.infoLines().set(index, content);
            } else {
              npc.infoLines().add(content);
            }
            updatedNpc = npc;
          }
        }

        // change the profile (will force-set the entity type to npc)
        case "profile" -> // load the profile
          updatedNpc = this.management.npcPlatform().profileResolver().resolveProfile(Profile.unresolved(args[2]))
            .thenApply(profile -> NPC.builder(npc)
              .profileProperties(profile.properties().stream()
                .map(prop -> new NPC.ProfileProperty(prop.name(), prop.value(), prop.signature()))
                .collect(Collectors.toSet()))
              .build())
            .exceptionally(throwable -> {
              sender.sendMessage(String.format("§cUnable to complete profile of §6%s§c!", args[2]));
              return null;
            }).join();

        // change the profile based on an image url
        case "urlprofile", "up" -> {
          sender.sendMessage("§7Trying to get texture data based on the given texture url...");
          Unirest.post("https://api.mineskin.org/generate/url")
            .requestTimeout(10000)
            .contentType("application/json")
            .header("User-Agent", "CloudNet-NPCs")
            .body(Document.newJsonDocument().append("url", args[2]).toString())
            .asStringAsync()
            .thenAccept(response -> {
              // check for success
              if (response.isSuccess()) {
                // extract the data we need from the body
                var textures = DocumentFactory.json().parse(response.getBody())
                  .readDocument("data")
                  .readDocument("texture");
                if (textures.empty()) {
                  // no data supplied by the api?
                  sender.sendMessage("§cApi request was successful but no data was submitted in the response!");
                  return;
                }

                // update & notify
                this.management.createNPC(NPC.builder(npc).profileProperties(Set.of(new NPC.ProfileProperty(
                  "textures",
                  textures.getString("value"),
                  textures.getString("signature")
                ))).build());
                sender.sendMessage(String.format(
                  "§7The option §6%s §7was updated §asuccessfully§7! It may take a few seconds for the change to become visible.",
                  StringUtil.toLower(args[1])));
              } else {
                // invalid response
                sender.sendMessage("§cUnable to convert the given string url to actual texture data!");
              }
            })
            .exceptionally(throwable -> {
              // send a message and log the exception
              sender.sendMessage(String.format("§cException while getting skin data: %s", throwable.getMessage()));
              this.plugin.getLogger().log(Level.SEVERE, "Exception getting mineskin response", throwable);
              // yep
              return null;
            });
          // async handling
          return true;
        }

        // change the entity type (will force-set the entity type to entity)
        case "et", "entitytype" -> {
          var entityType = Enums.getIfPresent(EntityType.class, StringUtil.toUpper(args[2])).orNull();
          if (entityType == null) {
            sender.sendMessage(String.format("§cNo such entity type: §6%s§c.", StringUtil.toUpper(args[2])));
            return true;
          }

          if (entityType.isAlive() && entityType.isSpawnable() && entityType != EntityType.PLAYER) {
            updatedNpc = NPC.builder(npc).entityType(entityType.name()).build();
          } else {
            sender.sendMessage(String.format("$cEntity type §6%s§c can not be spawned.", entityType));
            return true;
          }
        }

        case "in", "inventoryname" -> {
          // 3...: inventory name parts
          var inventoryName = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
          updatedNpc = NPC.builder(npc)
            .inventoryName(ChatColor.translateAlternateColorCodes('&', inventoryName))
            .build();
        }

        // sets the target group of the npc
        case "tg", "targetgroup" -> updatedNpc = NPC.builder(npc).targetGroup(args[2]).build();

        case "r", "rot", "rotate" -> {
          if (args.length < 4) {
            sender.sendMessage("§cInvalid usage! Use §6/cn edit rotate [~]<yaw/pitch> <value>§c!");
            return true;
          }

          // we want to use ~ as an operator to indicate whether the provided value is relative or absolute
          var input = args[3];
          var relative = input.startsWith("~");
          if (relative) {
            input = input.substring(1);
          }

          var position = npc.location();
          var value = Doubles.tryParse(input);
          if (value == null) {
            sender.sendMessage(String.format("§cUnable to parse yaw or pitch from input §6%s§c.", args[3]));
            return true;
          }

          WorldPosition updatedPosition;
          switch (StringUtil.toLower(args[2])) {
            case "yaw" -> {
              var yaw = relative ? position.yaw() + value : value;
              if (yaw < 0 || yaw > 360) {
                sender.sendMessage("§cInvalid argument! Yaw must be between 0 and 360, got " + yaw);
                return true;
              }

              updatedPosition = new WorldPosition(
                position.x(),
                position.y(),
                position.z(),
                yaw,
                position.pitch(),
                position.world(),
                position.group());
            }
            case "pitch" -> {
              var pitch = relative ? position.pitch() + value : value;
              if (pitch < -90 || pitch > 90) {
                sender.sendMessage("§cInvalid argument! Pitch must be between -90 and 90, got " + pitch);
                return true;
              }

              updatedPosition = new WorldPosition(
                position.x(),
                position.y(),
                position.z(),
                position.yaw(),
                pitch,
                position.world(),
                position.group());
            }
            default -> {
              sender.sendMessage("§cInvalid usage! Use §6/cn edit rotate [~]<yaw/pitch> <value>§c!");
              return true;
            }
          }

          updatedNpc = NPC.builder(npc).location(updatedPosition).build();
          // npcs use the position as key so the npc is not updated but rather another npc is created
          // just delete the old npc
          this.management.deleteNPC(npc);
        }

        // unknown option
        default -> {
          sender.sendMessage(String.format("§cNo option with name §6%s §cfound!", StringUtil.toLower(args[1])));
          return true;
        }
      }

      // update & notify if needed
      if (updatedNpc != null) {
        this.management.createNPC(updatedNpc);
        sender.sendMessage(String.format(
          "§7The option §6%s §7was updated §asuccessfully§7! It may take a few seconds for the change to become visible.",
          StringUtil.toLower(args[1])));
      }
      return true;
    }

    sender.sendMessage("§8> §7/cn create <targetGroup> <type> <skinOwnerName/entityType>");
    sender.sendMessage("§8> §7/cn edit <option> <value...>");
    sender.sendMessage("§8> §7/cn remove");
    sender.sendMessage("§8> §7/cn removeall [targetGroup]");
    sender.sendMessage("§8> §7/cn cleanup");
    sender.sendMessage("§8> §7/cn list");
    sender.sendMessage("§8> §7/cn <copy/cut/paste>");
    return true;
  }

  @Override
  public @NonNull Collection<String> tabComplete(@NonNull CommandSender sender, String @NonNull [] args) {
    // top level commands
    if (args.length == 1) {
      return Arrays.asList(
        "create",
        "edit",
        "remove",
        "removeall",
        "cleanup",
        "list",
        "copy",
        "cut",
        "paste",
        "clearclipboard");
    }
    // create arguments
    if (args[0].equalsIgnoreCase("create")) {
      switch (args.length) {
        case 2 -> {
          return this.groupConfigurationProvider.groupConfigurations().stream().map(GroupConfiguration::name).toList();
        }
        case 3 -> {
          return NPC_TYPES;
        }
        case 4 -> {
          // try to give a suggestion based on the previous input
          var type = Enums.getIfPresent(NPC.NPCType.class, StringUtil.toUpper(args[2])).orNull();
          if (type != null) {
            if (type == NPC.NPCType.ENTITY) {
              return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .filter(EntityType::isSpawnable)
                .filter(entityType -> entityType != EntityType.PLAYER)
                .map(Enum::name)
                .toList();
            } else {
              return Arrays.asList("derklaro", "juliarn", "0utplayyyy");
            }
          }
          return Collections.emptyList();
        }
        default -> {
          return Collections.emptyList();
        }
      }
    }
    // edit commands
    if (args[0].equalsIgnoreCase("edit")) {
      // top level options
      if (args.length == 2) {
        return Arrays.asList(
          "inventoryname",
          "rotate",
          "lookatplayer",
          "imitateplayer",
          "useplayerskin",
          "showingameservices",
          "showfullservices",
          "glowingcolor",
          "flyingwithelytra",
          "burning",
          "floatingitem",
          "leftclickaction",
          "rightclickaction",
          "items",
          "infolines",
          "profile",
          "entitytype",
          "targetgroup",
          "urlprofile");
      }
      // value options
      if (args.length == 3) {
        return switch (StringUtil.toLower(args[1])) {
          // true-false options
          case "lap", "lookatplayer", "ip", "imitateplayer", "ups", "useplayerskin",
               "fwe", "flyingwithelytra", "burning", "sis", "showingameservices", "sfs", "showfullservices" ->
            TRUE_FALSE;
          // click action options
          case "lca", "leftclickaction", "rca", "rightclickaction" -> CLICK_ACTIONS;
          // color options
          case "gc", "glowingcolor" -> Arrays.stream(ChatColor.values())
            .filter(ChatColor::isColor)
            .map(color -> String.valueOf(color.getChar()))
            .toList();
          // entity type
          case "et", "entitytype" -> Arrays.stream(EntityType.values())
            .filter(EntityType::isAlive)
            .filter(EntityType::isSpawnable)
            .filter(type -> type != EntityType.PLAYER)
            .map(Enum::name)
            .toList();
          // npc skin profile
          case "profile" -> Arrays.asList("derklaro", "juliarn", "0utplayyyy");
          // npc skin profile based on a texture url
          case "urlprofile", "up" -> List.of("https://i.nmc1.net/23f502a9f94379f1.png");
          // target group
          case "tg", "targetgroup" -> this.groupConfigurationProvider
            .groupConfigurations().stream()
            .map(GroupConfiguration::name)
            .toList();
          // rotation
          case "r", "rot", "rotate" -> List.of("yaw", "pitch");
          // item slots
          case "items" -> new ArrayList<>(VALID_ITEM_SLOTS.keySet());
          // info lines top level
          case "il", "infolines" -> Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
          // floating item
          case "fi", "floatingitem" -> MATERIAL_NAMES;
          // unknown or non-completable option
          default -> Collections.emptyList();
        };
      }
      // more...
      if (args.length == 4 && args[1].equalsIgnoreCase("items")) {
        return MATERIAL_NAMES;
      }
    }
    // unable to tab-complete
    return Collections.emptyList();
  }

  private @Nullable NPC getNearestNPC(@NonNull Location location) {
    return this.management.trackedEntities().values().stream()
      .filter(PlatformSelectorEntity::spawned)
      .filter(entity -> entity.location().getWorld().getUID().equals(location.getWorld().getUID()))
      .filter(entity -> entity.location().distanceSquared(location) <= 10)
      .min(Comparator.comparingDouble(entity -> entity.location().distanceSquared(location)))
      .map(PlatformSelectorEntity::npc)
      .orElse(null);
  }

  private boolean parseBoolean(@NonNull String input) {
    return input.contains("true") || input.contains("yes") || input.startsWith("y");
  }

  private boolean canChangeSetting(@NonNull CommandSender sender, @NonNull NPC npc) {
    if (npc.npcType() != NPC.NPCType.PLAYER) {
      sender.sendMessage(String.format("§cThis option is not available for the npc type §6%s§c!", npc.entityType()));
      return false;
    }
    return true;
  }
}
