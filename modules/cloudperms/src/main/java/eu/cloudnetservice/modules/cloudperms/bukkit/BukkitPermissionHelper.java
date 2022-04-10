/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.bukkit;

import static dev.derklaro.reflexion.matcher.FieldMatcher.newMatcher;

import dev.derklaro.reflexion.FieldAccessor;
import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

public final class BukkitPermissionHelper {

  private static final Pattern PACKAGE_VERSION_PATTERN = Pattern
    .compile("^org\\.bukkit\\.craftbukkit\\.(\\w+)\\.CraftServer$");

  private static final String SERVER_PACKAGE_VERSION;
  private static final FieldAccessor PERMISSIBLE_ACCESSOR;
  private static final MethodAccessor<?> UPDATE_COMMAND_TREE_ACCESSOR;

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

    // accessor to set the permissible of a player
    PERMISSIBLE_ACCESSOR = Reflexion.findAny(
        "org.bukkit.craftbukkit" + SERVER_PACKAGE_VERSION + "entity.CraftHumanEntity",
        "net.glowstone.entity.GlowHumanEntity"
      )
      .flatMap(reflexion -> reflexion.findField(newMatcher()
        .denyModifier(Modifier.STATIC)
        .derivedType(Field::getType, Permissible.class)))
      .orElseThrow(() -> new NoSuchElementException("Unable to resolve permissible field of player"));

    // accessor to re-send the command tree to a player
    UPDATE_COMMAND_TREE_ACCESSOR = Reflexion.on(Player.class).findMethod("updateCommands").orElse(null);
  }

  private BukkitPermissionHelper() {
    throw new UnsupportedOperationException();
  }

  public static void injectPlayer(@NonNull Player player) {
    PERMISSIBLE_ACCESSOR.setValue(
      player,
      new BukkitCloudPermissionsPermissible(player, CloudNetDriver.instance().permissionManagement()));
  }

  public static void resendCommandTree(@NonNull Player player) {
    if (UPDATE_COMMAND_TREE_ACCESSOR != null) {
      // just invoke and ignore the thrown exceptions
      UPDATE_COMMAND_TREE_ACCESSOR.invoke(player);
    }
  }

  public static boolean canUpdateCommandTree() {
    return UPDATE_COMMAND_TREE_ACCESSOR != null;
  }
}
