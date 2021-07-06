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

package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.listener.BukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.ApiStatus;

public final class BukkitCloudNetCloudPermissionsPlugin extends JavaPlugin {

  private static BukkitCloudNetCloudPermissionsPlugin instance;

  public static BukkitCloudNetCloudPermissionsPlugin getInstance() {
    return BukkitCloudNetCloudPermissionsPlugin.instance;
  }

  @Override
  public void onLoad() {
    instance = this;
  }

  @Override
  public void onEnable() {
    this.checkForVault();
    Bukkit.getOnlinePlayers().forEach(this::injectCloudPermissible);

    this.getServer().getPluginManager().registerEvents(new BukkitCloudNetCloudPermissionsPlayerListener(this,
      CloudNetDriver.getInstance().getPermissionManagement()), this);
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  @Deprecated
  public void updateNameTags(Player player) {
    this.updateNameTags(player, null);
  }

  @Deprecated
  public void updateNameTags(Player player, Function<Player, IPermissionGroup> playerIPermissionGroupFunction) {
    this.updateNameTags(player, playerIPermissionGroupFunction, null);
  }

  @Deprecated
  public void updateNameTags(Player player, Function<Player, IPermissionGroup> playerIPermissionGroupFunction,
    Function<Player, IPermissionGroup> allOtherPlayerPermissionGroupFunction) {
    Preconditions.checkNotNull(player);

    IPermissionUser playerPermissionUser = CloudNetDriver.getInstance().getPermissionManagement()
      .getUser(player.getUniqueId());
    AtomicReference<IPermissionGroup> playerPermissionGroup = new AtomicReference<>(
      playerIPermissionGroupFunction != null ? playerIPermissionGroupFunction.apply(player) : null);

    if (playerPermissionUser != null && playerPermissionGroup.get() == null) {
      playerPermissionGroup
        .set(CloudNetDriver.getInstance().getPermissionManagement().getHighestPermissionGroup(playerPermissionUser));

      if (playerPermissionGroup.get() == null) {
        playerPermissionGroup.set(CloudNetDriver.getInstance().getPermissionManagement().getDefaultPermissionGroup());
      }
    }

    int sortIdLength = CloudNetDriver.getInstance().getPermissionManagement().getGroups().stream()
      .map(IPermissionGroup::getSortId)
      .map(String::valueOf)
      .mapToInt(String::length)
      .max()
      .orElse(0);

    this.initScoreboard(player);

    Bukkit.getOnlinePlayers().forEach(all -> {
      this.initScoreboard(all);

      if (playerPermissionGroup.get() != null) {
        this.addTeamEntry(player, all, playerPermissionGroup.get(), sortIdLength);
      }

      IPermissionUser targetPermissionUser = CloudNetDriver.getInstance().getPermissionManagement()
        .getUser(all.getUniqueId());
      IPermissionGroup targetPermissionGroup =
        allOtherPlayerPermissionGroupFunction != null ? allOtherPlayerPermissionGroupFunction.apply(all) : null;

      if (targetPermissionUser != null && targetPermissionGroup == null) {
        targetPermissionGroup = CloudNetDriver.getInstance().getPermissionManagement()
          .getHighestPermissionGroup(targetPermissionUser);

        if (targetPermissionGroup == null) {
          targetPermissionGroup = CloudNetDriver.getInstance().getPermissionManagement().getDefaultPermissionGroup();
        }
      }

      if (targetPermissionGroup != null) {
        this.addTeamEntry(all, player, targetPermissionGroup, sortIdLength);
      }
    });
  }

  private void addTeamEntry(Player target, Player all, IPermissionGroup permissionGroup, int highestSortIdLength) {
    int sortIdLength = String.valueOf(permissionGroup.getSortId()).length();
    String teamName = (
      highestSortIdLength == sortIdLength ?
        permissionGroup.getSortId() :
        String.format("%0" + highestSortIdLength + "d", permissionGroup.getSortId())
    ) + permissionGroup.getName();

    if (teamName.length() > 16) {
      teamName = teamName.substring(0, 16);
    }

    Team team = all.getScoreboard().getTeam(teamName);
    if (team == null) {
      team = all.getScoreboard().registerNewTeam(teamName);
    }

    String prefix = permissionGroup.getPrefix();
    String color = permissionGroup.getColor();
    String suffix = permissionGroup.getSuffix();

    try {
      Method method = team.getClass().getDeclaredMethod("setColor", ChatColor.class);
      method.setAccessible(true);

      if (color != null && !color.isEmpty()) {
        ChatColor chatColor = ChatColor.getByChar(color.replaceAll("&", "").replaceAll("§", ""));
        if (chatColor != null) {
          method.invoke(team, chatColor);
        }
      } else {
        color = ChatColor.getLastColors(prefix.replace('&', '§'));
        if (!color.isEmpty()) {
          ChatColor chatColor = ChatColor.getByChar(color.replaceAll("&", "").replaceAll("§", ""));
          if (chatColor != null) {
            permissionGroup.setColor(color);
            CloudNetDriver.getInstance().getPermissionManagement().updateGroup(permissionGroup);
            method.invoke(team, chatColor);
          }
        }
      }
    } catch (NoSuchMethodException ignored) {
    } catch (IllegalAccessException | InvocationTargetException exception) {
      exception.printStackTrace();
    }

    team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));

    team.setSuffix(ChatColor.translateAlternateColorCodes('&', suffix));

    team.addEntry(target.getName());

    target.setDisplayName(ChatColor.translateAlternateColorCodes('&', permissionGroup.getDisplay() + target.getName()));
  }

  @ApiStatus.Internal
  public void injectCloudPermissible(Player player) {
    Preconditions.checkNotNull(player);
    try {
      BukkitPermissionInjectionHelper.injectPlayer(player);
    } catch (Throwable exception) {
      exception.printStackTrace();
    }
  }

  private void initScoreboard(Player all) {
    if (all.getScoreboard().equals(all.getServer().getScoreboardManager().getMainScoreboard())) {
      all.setScoreboard(all.getServer().getScoreboardManager().getNewScoreboard());
    }
  }

  private void checkForVault() {
    if (super.getServer().getPluginManager().isPluginEnabled("Vault")
      || super.getServer().getPluginManager().isPluginEnabled("VaultAPI")) {

      try {
        Class<?> vaultSupportClass = Class.forName("de.dytanic.cloudnet.ext.cloudperms.bukkit.vault.VaultSupport");
        Method enableMethod = vaultSupportClass
          .getDeclaredMethod("enable", JavaPlugin.class, IPermissionManagement.class);

        enableMethod.invoke(null, this, CloudNetDriver.getInstance().getPermissionManagement());

        super.getLogger().info("Enabled Vault support!");
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
        exception.printStackTrace();
      }
    }
  }
}
