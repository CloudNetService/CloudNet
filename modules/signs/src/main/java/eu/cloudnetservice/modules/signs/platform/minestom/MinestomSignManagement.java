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

package eu.cloudnetservice.modules.signs.platform.minestom;

import com.google.common.util.concurrent.MoreExecutors;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.bridge.platform.minestom.MinestomInstanceProvider;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

public class MinestomSignManagement extends PlatformSignManagement<Player, Pair<Point, Instance>, String> {

  private final MinestomInstanceProvider provider = new MinestomInstanceProvider();

  protected MinestomSignManagement() {
    super(MoreExecutors.directExecutor());
  }

  @Override
  protected int tps() {
    return MinecraftServer.TICK_PER_SECOND;
  }

  @Override
  protected void startKnockbackTask() {
    MinecraftServer.getSchedulerManager().scheduleTask(() -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          var distance = conf.distance();
          // find all signs which need to knock back the player
          for (var sign : this.platformSigns.values()) {
            if (sign.needsUpdates() && sign.exists() && sign instanceof MinestomPlatformSign minestomSigns) {
              var location = minestomSigns.signLocation();
              if (location != null) {
                var vec = location.first().asVec();
                for (var entity : location.second().getNearbyEntities(location.first(), distance)) {
                  if (entity instanceof Player player && !(entity instanceof FakePlayer)
                    && (conf.bypassPermission() == null || !player.hasPermission(conf.bypassPermission()))) {
                    entity.setVelocity(entity.getPosition().asVec()
                      .sub(vec)
                      .normalize()
                      .mul(conf.strength())
                      .withY(0.2));
                  }
                }
              }
            }
          }
        }
      }
    }, TaskSchedule.immediate(), TaskSchedule.tick(5));
  }

  @Override
  public @Nullable WorldPosition convertPosition(@NonNull Pair<Point, Instance> location) {
    return this.convertPosition(location.first(), location.second());
  }

  public @Nullable WorldPosition convertPosition(@NonNull Point pos, @NonNull Instance instance) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry == null) {
      return null;
    }
    // extract the name of the instance
    var identifier = this.provider.extractInstanceIdentifier(instance);
    return new WorldPosition(pos.x(), pos.y(), pos.z(), 0, 0, identifier, entry.targetGroup());
  }


  @Override
  protected @NonNull PlatformSign<Player, String> createPlatformSign(@NonNull Sign base) {
    return new MinestomPlatformSign(base, this.provider);
  }
}
