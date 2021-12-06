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

package de.dytanic.cloudnet.ext.bridge.platform.bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class BukkitUtil {

  private static final Function<Player, String> LOCALE_GETTER;

  static {
    Function<Player, String> localeGetter;
    try {
      // legacy bukkit
      Player.Spigot.class.getMethod("getLocale");
      localeGetter = player -> player.spigot().getLocale();
    } catch (NoSuchMethodException exception) {
      try {
        // modern bukkit
        MethodHandle getLocale = MethodHandles.publicLookup().findVirtual(
          Player.class,
          "getLocale",
          MethodType.methodType(String.class));
        localeGetter = player -> {
          try {
            return (String) getLocale.invoke(player);
          } catch (Throwable throwable) {
            return null;
          }
        };
      } catch (NoSuchMethodException | IllegalAccessException ignored) {
        localeGetter = $ -> null;
      }
    }
    LOCALE_GETTER = localeGetter;
  }

  public static @NotNull String getPlayerLocale(@NotNull Player player) {
    return LOCALE_GETTER.apply(player);
  }
}
