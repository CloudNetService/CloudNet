package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.listener.BukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
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
        this.checkForVault(CloudPermissionsManagement.getInstance());
        this.initPlayersCloudPermissible();

        getServer().getPluginManager().registerEvents(new BukkitCloudNetCloudPermissionsPlayerListener(), this);
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    public void updateNameTags(Player player) {
        updateNameTags(player, null);
    }

    public void updateNameTags(Player player, Function<Player, IPermissionGroup> playerIPermissionGroupFunction) {
        updateNameTags(player, playerIPermissionGroupFunction, null);
    }

    public void updateNameTags(Player player, Function<Player, IPermissionGroup> playerIPermissionGroupFunction,
                               Function<Player, IPermissionGroup> allOtherPlayerPermissionGroupFunction) {
        Preconditions.checkNotNull(player);

        IPermissionUser playerPermissionUser = CloudPermissionsManagement.getInstance().getUser(player.getUniqueId());
        AtomicReference<IPermissionGroup> playerPermissionGroup = new AtomicReference<>(playerIPermissionGroupFunction != null ? playerIPermissionGroupFunction.apply(player) : null);

        if (playerPermissionUser != null && playerPermissionGroup.get() == null) {
            playerPermissionGroup.set(CloudPermissionsManagement.getInstance().getHighestPermissionGroup(playerPermissionUser));

            if (playerPermissionGroup.get() == null) {
                playerPermissionGroup.set(CloudPermissionsManagement.getInstance().getDefaultPermissionGroup());
            }
        }

        initScoreboard(player);

        Bukkit.getOnlinePlayers().forEach(all -> {
            initScoreboard(all);

            if (playerPermissionGroup.get() != null) {
                addTeamEntry(player, all, playerPermissionGroup.get());
            }

            IPermissionUser targetPermissionUser = CloudPermissionsManagement.getInstance().getUser(all.getUniqueId());
            IPermissionGroup targetPermissionGroup = allOtherPlayerPermissionGroupFunction != null ? allOtherPlayerPermissionGroupFunction.apply(all) : null;

            if (targetPermissionUser != null && targetPermissionGroup == null) {
                targetPermissionGroup = CloudPermissionsManagement.getInstance().getHighestPermissionGroup(targetPermissionUser);

                if (targetPermissionGroup == null) {
                    targetPermissionGroup = CloudPermissionsManagement.getInstance().getDefaultPermissionGroup();
                }
            }

            if (targetPermissionGroup != null) {
                addTeamEntry(all, player, targetPermissionGroup);
            }
        });
    }

    private void addTeamEntry(Player target, Player all, IPermissionGroup permissionGroup) {
        String teamName = permissionGroup.getSortId() + permissionGroup.getName();

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
                        CloudPermissionsManagement.getInstance().updateGroup(permissionGroup);
                        method.invoke(team, chatColor);
                    }
                }
            }
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
        }

        team.setPrefix(ChatColor.translateAlternateColorCodes('&',
                prefix.length() > 16 ?
                        prefix.substring(0, 16) : prefix));

        team.setSuffix(ChatColor.translateAlternateColorCodes('&',
                suffix.length() > 16 ?
                        suffix.substring(0, 16) : suffix));

        team.addEntry(target.getName());

        target.setDisplayName(ChatColor.translateAlternateColorCodes('&', permissionGroup.getDisplay() + target.getName()));
    }

    public void injectCloudPermissible(Player player) {
        Preconditions.checkNotNull(player);

        try {
            Field field;
            Class<?> clazz = reflectCraftClazz(".entity.CraftHumanEntity");

            if (clazz != null) {
                field = clazz.getDeclaredField("perm");
            } else {
                field = Class.forName("net.glowstone.entity.GlowHumanEntity").getDeclaredField("permissions");
            }

            injectCloudPermissible0(player, field);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    private void initScoreboard(Player all) {
        if (all.getScoreboard().equals(all.getServer().getScoreboardManager().getMainScoreboard())) {
            all.setScoreboard(all.getServer().getScoreboardManager().getNewScoreboard());
        }
    }

    private void injectCloudPermissible0(Player player, Field field) throws Exception {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(field);

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
                return null;
            }
        }
    }

    private void initPlayersCloudPermissible() {
        Bukkit.getOnlinePlayers().forEach(this::injectCloudPermissible);
    }

    private void checkForVault(IPermissionManagement permissionManagement) {
        if (super.getServer().getPluginManager().isPluginEnabled("Vault")
                || super.getServer().getPluginManager().isPluginEnabled("VaultAPI")) {

            try {

                Class<?> vaultSupportClass = Class.forName("de.dytanic.cloudnet.ext.cloudperms.bukkit.vault.VaultSupport");
                Method enableMethod = vaultSupportClass.getDeclaredMethod("enable", JavaPlugin.class, IPermissionManagement.class);

                enableMethod.invoke(null, this, permissionManagement);

                super.getLogger().info("Enabled Vault support!");

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }

        }
    }

}