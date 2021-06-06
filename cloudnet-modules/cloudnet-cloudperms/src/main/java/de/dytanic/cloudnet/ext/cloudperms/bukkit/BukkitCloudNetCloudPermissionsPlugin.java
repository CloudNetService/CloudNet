package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.listener.BukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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

    @ApiStatus.Internal
    public void injectCloudPermissible(Player player) {
        Preconditions.checkNotNull(player);
        try {
            BukkitPermissionInjectionHelper.injectPlayer(player);
        } catch (Throwable exception) {
            exception.printStackTrace();
        }
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
