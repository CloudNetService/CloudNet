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

package eu.cloudnetservice.modules.bridge.platform.bukkit;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;
import lombok.NonNull;
import org.bukkit.entity.Player;

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
        var getLocale = MethodHandles.publicLookup().findVirtual(
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

  private BukkitUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String playerLocale(@NonNull Player player) {
    return LOCALE_GETTER.apply(player);
  }
}
