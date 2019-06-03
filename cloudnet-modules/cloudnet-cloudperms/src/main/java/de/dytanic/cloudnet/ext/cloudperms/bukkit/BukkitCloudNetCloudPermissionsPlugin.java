package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.listener.BukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

public final class BukkitCloudNetCloudPermissionsPlugin extends JavaPlugin {

  @Getter
  private static BukkitCloudNetCloudPermissionsPlugin instance;

  @Override
  public void onLoad() {
    instance = this;
  }

  @Override
  public void onEnable() {
    new CloudPermissionsPermissionManagement();
    initPlayersCloudPermissible();

    getServer().getPluginManager()
        .registerEvents(new BukkitCloudNetCloudPermissionsPlayerListener(),
            this);
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager()
        .unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(
        this.getClass().getClassLoader());
  }

  public void updateNameTags(Player player) {
    updateNameTags(player, null);
  }

  public void updateNameTags(Player player,
      Function<Player, IPermissionGroup> playerIPermissionGroupFunction) {
    updateNameTags(player, playerIPermissionGroupFunction, null);
  }

  public void updateNameTags(Player player,
      Function<Player, IPermissionGroup> playerIPermissionGroupFunction,
      Function<Player, IPermissionGroup> allOtherPlayerPermissionGroupFunction) {
    Validate.checkNotNull(player);

    IPermissionUser playerPermissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(player.getUniqueId());
    Value<IPermissionGroup> playerPermissionGroup = new Value<>(
        playerIPermissionGroupFunction != null ? playerIPermissionGroupFunction
            .apply(player) : null);

    if (playerPermissionUser != null
        && playerPermissionGroup.getValue() == null) {
      playerPermissionGroup.setValue(
          CloudPermissionsPermissionManagement.getInstance()
              .getHighestPermissionGroup(playerPermissionUser));

      if (playerPermissionGroup.getValue() == null) {
        playerPermissionGroup.setValue(
            CloudPermissionsPermissionManagement.getInstance()
                .getDefaultPermissionGroup());
      }
    }

    initScoreboard(player);

    forEachPlayers(new Consumer<Player>() {
      @Override
      public void accept(Player all) {
        initScoreboard(all);

        if (playerPermissionGroup.getValue() != null) {
          addTeamEntry(player, all, playerPermissionGroup.getValue());
        }

        IPermissionUser targetPermissionUser = CloudPermissionsPermissionManagement
            .getInstance().getUser(all.getUniqueId());
        IPermissionGroup targetPermissionGroup =
            allOtherPlayerPermissionGroupFunction != null
                ? allOtherPlayerPermissionGroupFunction.apply(all) : null;

        if (targetPermissionUser != null && targetPermissionGroup == null) {
          targetPermissionGroup = CloudPermissionsPermissionManagement
              .getInstance().getHighestPermissionGroup(targetPermissionUser);

          if (targetPermissionGroup == null) {
            targetPermissionGroup = CloudPermissionsPermissionManagement
                .getInstance().getDefaultPermissionGroup();
          }
        }

        if (targetPermissionGroup != null) {
          addTeamEntry(all, player, targetPermissionGroup);
        }
      }
    });
  }

  private void addTeamEntry(Player target, Player all,
      IPermissionGroup permissionGroup) {
    String teamName = permissionGroup.getSortId() + permissionGroup.getName();

    try {
      if (teamName.length() > 16) {
        teamName = shortenStringTo16Bytes(teamName);
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    Team team = all.getScoreboard().getTeam(teamName);
    if (team == null) {
      team = all.getScoreboard().registerNewTeam(teamName);
    }

    try {
      team.setPrefix(ChatColor.translateAlternateColorCodes('&',
          permissionGroup.getPrefix().length() > 16 ?
              shortenStringTo16Bytes(permissionGroup.getPrefix())
              : permissionGroup.getPrefix()));

      team.setSuffix(ChatColor.translateAlternateColorCodes('&',
          permissionGroup.getSuffix().length() > 16 ?
              shortenStringTo16Bytes(permissionGroup.getSuffix())
              : permissionGroup.getSuffix()));

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    team.addPlayer(target);

    target.setDisplayName(ChatColor.translateAlternateColorCodes('&',
        permissionGroup.getDisplay() + target.getName()));
  }

  public void injectCloudPermissible(Player player) {
    Validate.checkNotNull(player);

    try {
      Field field;
      Class<?> clazz = reflectCraftClazz(".entity.CraftHumanEntity");

      if (clazz != null) {
        field = clazz.getDeclaredField("perm");
      } else {
        field = Class.forName("net.glowstone.entity.GlowHumanEntity")
            .getDeclaredField("permissions");
      }

      injectCloudPermissible0(player, field);
    } catch (Exception ignored) {
    }
  }

  /*= --------------------------------------------------------------------------------------------------- =*/

  private String shortenStringTo16Bytes(String input)
      throws UnsupportedEncodingException {
    return input.substring(0, 16);
  }

  private void initScoreboard(Player all) {
    if (all.getScoreboard() == null) {
      all.setScoreboard(
          all.getServer().getScoreboardManager().getNewScoreboard());
    }
  }

  private void injectCloudPermissible0(Player player, Field field)
      throws Exception {
    Validate.checkNotNull(player);
    Validate.checkNotNull(field);

    field.setAccessible(true);
    field.set(player, new BukkitCloudNetCloudPermissionsPermissible(player));
  }

  private Class<?> reflectCraftClazz(String suffix) {
    try {
      String version = org.bukkit.Bukkit.getServer().getClass().getPackage()
          .getName().split("\\.")[3];
      return Class.forName("org.bukkit.craftbukkit." + version + suffix);
    } catch (Exception ex) {
      try {
        return Class.forName("org.bukkit.craftbukkit." + suffix);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return null;
  }

  private void forEachPlayers(Consumer<Player> consumer) {
    Method method;
    try {
      method = Server.class.getMethod("getOnlinePlayers");
      method.setAccessible(true);
      Object result = method.invoke(Bukkit.getServer());

      if (result instanceof Iterable) {
        for (Object item : ((Iterable) result)) {
          consumer.accept((Player) item);
        }
        return;
      }

      if (result instanceof Player[]) {
        for (Player player : ((Player[]) result)) {
          consumer.accept(player);
        }
      }

    } catch (Exception ignored) {
    }
  }

  private void initPlayersCloudPermissible() {
    forEachPlayers(new Consumer<Player>() {
      @Override
      public void accept(Player player) {
        injectCloudPermissible(player);
      }
    });
  }
}