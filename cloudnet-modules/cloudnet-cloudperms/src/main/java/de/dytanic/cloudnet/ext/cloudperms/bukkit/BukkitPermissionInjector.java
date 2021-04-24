package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BukkitPermissionInjector {

    private static final Pattern PACKAGE_VERSION_PATTERN = Pattern.compile("^org\\.bukkit\\.craftbukkit\\.(\\w+)\\.CraftServer$");

    private static final Field PERMISSIBLE_FIELD;
    private static final String SERVER_PACKAGE_VERSION;

    static {
        // load server version
        Matcher matcher = PACKAGE_VERSION_PATTERN.matcher(Bukkit.getServer().getClass().getName());
        if (matcher.matches()) {
            // the server package name is versioned
            SERVER_PACKAGE_VERSION = '.' + matcher.group(1) + '.';
        } else {
            // the server package is not versioned (custom forks are often not versioned)
            SERVER_PACKAGE_VERSION = ".";
        }

        try {
            Field permissibleField;
            try {
                // bukkit
                permissibleField = Class.forName("org.bukkit.craftbukkit" + SERVER_PACKAGE_VERSION + "entity.CraftHumanEntity").getDeclaredField("perm");
                permissibleField.setAccessible(true);
            } catch (Exception e) {
                // glowstone
                permissibleField = Class.forName("net.glowstone.entity.GlowHumanEntity").getDeclaredField("permissions");
                permissibleField.setAccessible(true);
            }

            PERMISSIBLE_FIELD = permissibleField;
        } catch (final ClassNotFoundException | NoSuchFieldException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private BukkitPermissionInjector() {
        throw new UnsupportedOperationException();
    }

    public static void injectPlayer(@NotNull Player player) throws Exception {
        Preconditions.checkNotNull(player, "player");
        injectPlayer(player, PERMISSIBLE_FIELD);
    }

    public static void injectPlayer(@NotNull Player player, @NotNull Field field) throws Exception {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(field, "field");

        field.set(player,
                new BukkitCloudNetCloudPermissionsPermissible(player, CloudNetDriver.getInstance().getPermissionManagement()));
    }
}
