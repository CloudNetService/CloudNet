package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.listener.BukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class BukkitCloudNetCloudPermissionsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.checkForVault();
        this.initPlayersCloudPermissible();

        this.getServer().getPluginManager().registerEvents(new BukkitCloudNetCloudPermissionsPlayerListener(this, CloudNetDriver.getInstance().getPermissionManagement()), this);
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    public void injectCloudPermissible(Player player) {
        Preconditions.checkNotNull(player);

        try {
            Field field;
            Class<?> clazz = this.reflectCraftClazz(".entity.CraftHumanEntity");

            if (clazz != null) {
                field = clazz.getDeclaredField("perm");
            } else {
                field = Class.forName("net.glowstone.entity.GlowHumanEntity").getDeclaredField("permissions");
            }

            this.injectCloudPermissible0(player, field);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void injectCloudPermissible0(Player player, Field field) throws Exception {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(field);

        field.setAccessible(true);
        field.set(player, new BukkitCloudNetCloudPermissionsPermissible(player, CloudNetDriver.getInstance().getPermissionManagement()));
    }

    private Class<?> reflectCraftClazz(String suffix) {
        try {
            String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return Class.forName("org.bukkit.craftbukkit." + version + suffix);
        } catch (Exception ex) {
            try {
                return Class.forName("org.bukkit.craftbukkit." + suffix);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }
    }

    private void initPlayersCloudPermissible() {
        Bukkit.getOnlinePlayers().forEach(this::injectCloudPermissible);
    }

    private void checkForVault() {
        if (super.getServer().getPluginManager().isPluginEnabled("Vault")
                || super.getServer().getPluginManager().isPluginEnabled("VaultAPI")) {

            try {
                Class<?> vaultSupportClass = Class.forName("de.dytanic.cloudnet.ext.cloudperms.bukkit.vault.VaultSupport");
                Method enableMethod = vaultSupportClass.getDeclaredMethod("enable", JavaPlugin.class, IPermissionManagement.class);

                enableMethod.invoke(null, this, CloudNetDriver.getInstance().getPermissionManagement());

                super.getLogger().info("Enabled Vault support!");
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }
    }
}
