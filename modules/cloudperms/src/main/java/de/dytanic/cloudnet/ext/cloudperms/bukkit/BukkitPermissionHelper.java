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

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BukkitPermissionHelper {

  private static final Pattern PACKAGE_VERSION_PATTERN = Pattern
    .compile("^org\\.bukkit\\.craftbukkit\\.(\\w+)\\.CraftServer$");
  private static final Logger LOGGER = LogManager.getLogger(BukkitPermissionHelper.class);

  private static final String SERVER_PACKAGE_VERSION;
  private static final MethodHandle PERMISSIBLE_SETTER;
  private static final MethodHandle UPDATE_COMMAND_TREE;

  static {
    // load server version
    var matcher = PACKAGE_VERSION_PATTERN.matcher(Bukkit.getServer().getClass().getName());
    if (matcher.matches()) {
      // the server package name is versioned
      SERVER_PACKAGE_VERSION = '.' + matcher.group(1) + '.';
    } else {
      // the server package is not versioned (custom forks are often not versioned)
      SERVER_PACKAGE_VERSION = ".";
    }

    // find the permissible field
    try {
      Field permissibleField;
      try {
        // bukkit
        permissibleField = Class.forName("org.bukkit.craftbukkit" + SERVER_PACKAGE_VERSION + "entity.CraftHumanEntity")
          .getDeclaredField("perm");
        permissibleField.setAccessible(true);
      } catch (Exception e) {
        // glowstone
        permissibleField = Class.forName("net.glowstone.entity.GlowHumanEntity").getDeclaredField("permissions");
        permissibleField.setAccessible(true);
      }

      PERMISSIBLE_SETTER = MethodHandles.lookup().unreflectSetter(permissibleField);
    } catch (final ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }

    // find the updateCommands method
    MethodHandle updateCommands;
    try {
      updateCommands = MethodHandles.publicLookup().findVirtual(
        Player.class,
        "updateCommands",
        MethodType.methodType(void.class));
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      updateCommands = null;
    }
    // assign the static field
    UPDATE_COMMAND_TREE = updateCommands;
  }

  private BukkitPermissionHelper() {
    throw new UnsupportedOperationException();
  }

  public static void injectPlayer(@NotNull Player player) throws Throwable {
    PERMISSIBLE_SETTER.invoke(
      player,
      new BukkitCloudPermissionsPermissible(player, CloudNetDriver.getInstance().getPermissionManagement()));
  }

  public static void resendCommandTree(@NotNull Player player) {
    if (UPDATE_COMMAND_TREE != null) {
      try {
        UPDATE_COMMAND_TREE.invoke(player);
      } catch (Throwable throwable) {
        LOGGER.severe("Exception resending player command tree", throwable);
      }
    }
  }

  public static boolean canUpdateCommandTree() {
    return UPDATE_COMMAND_TREE != null;
  }
}
