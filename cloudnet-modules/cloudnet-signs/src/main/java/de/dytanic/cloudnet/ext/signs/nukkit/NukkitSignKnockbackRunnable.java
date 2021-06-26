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

package de.dytanic.cloudnet.ext.signs.nukkit;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.Sign;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NukkitSignKnockbackRunnable implements Runnable {

  private final Map<WorldPosition, Location> signLocations = new HashMap<>();

  private final NukkitSignManagement nukkitSignManagement;

  NukkitSignKnockbackRunnable(NukkitSignManagement nukkitSignManagement) {
    this.nukkitSignManagement = nukkitSignManagement;
  }

  @Override
  public void run() {
    Set<Sign> signs = this.nukkitSignManagement.getSigns();

    for (Sign sign : signs) {
      Location signLocation = this.signLocations
        .computeIfAbsent(sign.getWorldPosition(), this.nukkitSignManagement::toLocation);

      if (signLocation != null && signLocation.getLevel() != null) {
        double knockbackDistance = this.nukkitSignManagement.getOwnSignConfigurationEntry().getKnockbackDistance();

        AxisAlignedBB boundingBox = new SimpleAxisAlignedBB(signLocation, signLocation)
          .expand(knockbackDistance, knockbackDistance, knockbackDistance);

        Arrays.stream(signLocation.getLevel().getNearbyEntities(boundingBox))
          .filter(
            entity -> entity instanceof Player && !((Player) entity).hasPermission("cloudnet.signs.knockback.bypass"))
          .forEach(player -> {
            // pushing the player back with the specified strength
            Vector3 vector3 = player.getPosition().subtract(signLocation)
              .normalize()
              .multiply(this.nukkitSignManagement.getOwnSignConfigurationEntry().getKnockbackStrength());
            vector3.y = 0.2;

            player.setMotion(vector3);
          });
      }
    }

  }


}
