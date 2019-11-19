package de.dytanic.cloudnet.ext.cloudperms.bukkit.vault;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultSupport {

    public static void enable(JavaPlugin plugin, IPermissionManagement permissionManagement) {
        ServicesManager servicesManager = plugin.getServer().getServicesManager();

        Permission vaultPermissionImplementation = new VaultPermissionImplementation(permissionManagement);

        servicesManager.register(Permission.class, vaultPermissionImplementation, plugin, ServicePriority.Highest);
        servicesManager.register(Chat.class, new VaultChatImplementation(vaultPermissionImplementation, permissionManagement), plugin, ServicePriority.Highest);
    }

}
