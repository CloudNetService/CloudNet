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

package eu.cloudnetservice.plugins.simplenametags.bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

final class BukkitCompatibility {

  private static final MethodHandle SET_COLOR;

  static {
    MethodHandle setColor;
    // the method will only be available for 1.13+ server
    try {
      setColor = MethodHandles.lookup().findVirtual(
        Team.class,
        "setColor",
        MethodType.methodType(void.class, ChatColor.class));
    } catch (NoSuchMethodException | IllegalAccessException expected) {
      // 1.8 to 1.12 server
      setColor = null;
    }
    // set the delegate holding field
    SET_COLOR = setColor;
  }

  private BukkitCompatibility() {
    throw new UnsupportedOperationException();
  }

  public static void teamColor(@NonNull Team team, @NonNull ChatColor color) {
    // check if the method is available
    if (SET_COLOR != null) {
      // set the team color
      try {
        SET_COLOR.invoke(team, color);
      } catch (Throwable throwable) {
        throw new IllegalStateException("Team#setColor must succeed on 1.13+ servers", throwable);
      }
    }
  }
}
