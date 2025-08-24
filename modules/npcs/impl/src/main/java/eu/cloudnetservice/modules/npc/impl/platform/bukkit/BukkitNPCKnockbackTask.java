/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit;

import com.github.juliarn.npclib.ext.labymod.LabyModExtension;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.entity.NPCBukkitPlatformSelector;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 *
 */
final class BukkitNPCKnockbackTask implements Runnable {

  private static final double DISTANCE_EPS = 1e-6D;
  private static final double KNOCKBACK_UPWARDS_VELOCITY = 0.2D;
  private static final String KNOCKBACK_BYPASS_PERM = "cloudnet.npcs.knockback.bypass";

  private final BukkitPlatformNPCManagement npcManagement;

  public BukkitNPCKnockbackTask(@NonNull BukkitPlatformNPCManagement npcManagement) {
    this.npcManagement = npcManagement;
  }

  /**
   * Returns a randomized vector in case the given vector is very small.
   *
   * @param vector the input vector to check.
   * @return a new vector if the given vector is very small, else the given vector.
   */
  private static @NonNull Vector randomizeSmallVector(@NonNull Vector vector) {
    if (vector.lengthSquared() <= DISTANCE_EPS) {
      // rare, but possible case: the player stands very close to the position of the npc. in that case
      // the vector ops could make the knockback direction undefined. to circumvent we just use a unit vector
      var random = ThreadLocalRandom.current();
      var angle = random.nextDouble(0D, 2D * Math.PI);
      vector = new Vector(Math.cos(angle), 0, Math.sin(angle));
    }

    return vector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    var configEntry = this.npcManagement.applicableNPCConfigurationEntry();
    if (configEntry == null) {
      return;
    }

    // check if knockback is enabled
    var distance = configEntry.knockbackDistance();
    var strength = configEntry.knockbackStrength();
    if (distance <= 0 || strength <= 0) {
      return;
    }

    // select an emote id for the knockback animation - this only returns a real emote id in
    // case it's configured to sync the same emote across all displayed NPCs
    var random = ThreadLocalRandom.current();
    var labyModEmotes = configEntry.emoteConfiguration().onKnockbackEmoteIds();
    var emoteId = this.npcManagement.randomEmoteId(configEntry.emoteConfiguration(), labyModEmotes);

    for (var entity : this.npcManagement.trackedEntities().values()) {
      if (!entity.spawned()) {
        continue;
      }

      var npcPosition = entity.location();
      var npcPositionVector = npcPosition.toVector();
      var nearbyEntities = npcPosition.getWorld().getNearbyEntities(npcPosition, distance, distance, distance);
      if (nearbyEntities.isEmpty()) {
        continue;
      }

      for (var nearbyEntity : nearbyEntities) {
        if (!(nearbyEntity instanceof Player player) || player.hasPermission(KNOCKBACK_BYPASS_PERM)) {
          continue;
        }

        var playerPositionVector = player.getLocation().toVector();
        var vectorTowardsPlayer = playerPositionVector.subtract(npcPositionVector);
        var safeVector = randomizeSmallVector(vectorTowardsPlayer);
        var knockbackVelocity = safeVector.normalize().multiply(strength).setY(KNOCKBACK_UPWARDS_VELOCITY);
        player.setVelocity(knockbackVelocity);

        // display a knockback LabyMod emote to the player, if any was selected
        if (entity instanceof NPCBukkitPlatformSelector npcSelector && emoteId >= -1) {
          var libNpc = npcSelector.handleNPC();
          var npcPlatform = this.npcManagement.npcPlatform();
          var emoteIdToSend = switch (emoteId) {
            case -1 -> labyModEmotes[random.nextInt(0, labyModEmotes.length)];
            case int id -> id;
          };
          LabyModExtension.createEmotePacket(npcPlatform.packetFactory(), emoteIdToSend).schedule(player, libNpc);
        }
      }
    }
  }
}
