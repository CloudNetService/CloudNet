package de.dytanic.cloudnet.ext.simplenametags.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.simplenametags.CloudNetSimpleNameTagsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CloudNetSimpleNameTagsListener implements Listener {

  private final CloudNetSimpleNameTagsPlugin plugin;

  public CloudNetSimpleNameTagsListener(CloudNetSimpleNameTagsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTask(this.plugin, () -> plugin.updateNameTags(event.getPlayer()));
  }

  @EventListener
  public void handle(PermissionUpdateUserEvent event) {
    Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getOnlinePlayers().stream()
      .filter(player -> player.getUniqueId().equals(event.getPermissionUser().getUniqueId()))
      .findFirst()
      .ifPresent(plugin::updateNameTags));
  }

  @EventListener
  public void handle(PermissionUpdateGroupEvent event) {
    Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getOnlinePlayers().forEach(player -> {
      IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement()
        .getUser(player.getUniqueId());

      if (permissionUser != null && permissionUser.inGroup(event.getPermissionGroup().getName())) {
        plugin.updateNameTags(player);
      }
    }));
  }

}
