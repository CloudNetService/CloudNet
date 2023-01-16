/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

final class BukkitCompatibility {

  private static final MethodAccessor<?> SET_COLOR = Reflexion.on(Team.class)
    .findMethod("setColor", ChatColor.class)
    .orElse(null);

  private BukkitCompatibility() {
    throw new UnsupportedOperationException();
  }

  public static void teamColor(@NonNull Team team, @NonNull ChatColor color) {
    // check if the method is available
    if (SET_COLOR != null) {
      // set the team color
      SET_COLOR.invoke(team, color);
    }
  }
}
