package eu.cloudnetservice.cloudnet.ext.bridge.chat;

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CloudNetChatPlugin extends JavaPlugin implements Listener {

    private String format;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.format = getConfig().getString("format");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        IPermissionUser user = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());
        IPermissionGroup group = CloudPermissionsPermissionManagement.getInstance().getHighestPermissionGroup(user);

        String message = event.getMessage().replace("%", "%%");
        if (player.hasPermission("cloudnet.chat.color"))
            message = ChatColor.translateAlternateColorCodes('&', message);

        if (ChatColor.stripColor(message).trim().isEmpty()) {
            event.setCancelled(true);
            return;
        }

        event.setFormat(
                ChatColor.translateAlternateColorCodes(
                        '&',
                        this.format
                                .replace("%group%", group.getName())
                                .replace("%display%", group.getDisplay())
                                .replace("%prefix%", group.getPrefix())
                                .replace("%suffix%", group.getSuffix())
                                .replace("%color%", group.getColor())
                                .replace("%name%", player.getName())
                                .replace("%uniqueId%", player.getUniqueId().toString())
                ).replace("%message%", message)
        );

    }

}
