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

package eu.cloudnetservice.modules.signs.platform.sponge;

import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

public class SpongeSignManagement extends PlatformSignManagement<ServerPlayer, ServerLocation, Component> {

  protected final PluginContainer plugin;
  protected final TaskExecutorService syncExecutor;

  protected SpongeSignManagement(@NonNull PluginContainer plugin, @NonNull TaskExecutorService mainThreadExecutor) {
    super(runnable -> {
      // check if we're already on main
      if (Sponge.server().onMainThread()) {
        runnable.run();
      } else {
        mainThreadExecutor.execute(runnable);
      }
    });
    this.plugin = plugin;
    this.syncExecutor = mainThreadExecutor;
  }

  public static @NonNull SpongeSignManagement newInstance(@NonNull PluginContainer plugin) {
    var executor = Sponge.server().scheduler().executor(plugin);
    return new SpongeSignManagement(plugin, executor);
  }

  @Override
  protected int tps() {
    return 20;
  }

  @Override
  protected void startKnockbackTask() {
    this.syncExecutor.scheduleWithFixedDelay(() -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          var distance = conf.distance();
          // find all signs which need to knock back the player
          for (var sign : this.platformSigns.values()) {
            if (sign.needsUpdates() && sign.exists() && sign instanceof SpongePlatformSign spongeSign) {
              var location = spongeSign.signLocation();
              if (location != null) {
                var locationVec = location.position();
                for (Entity entity : location.world().nearbyEntities(locationVec, distance)) {
                  if (entity instanceof ServerPlayer player
                    && (conf.bypassPermission() == null || !player.hasPermission(conf.bypassPermission()))) {
                    var vector = entity.location()
                      .position()
                      .sub(locationVec)
                      .normalize()
                      .mul(conf.strength());
                    entity.velocity().set(new Vector3d(vector.x(), 0.2D, vector.z()));
                  }
                }
              }
            }
          }
        }
      }
    }, 0, 5 * 50, TimeUnit.MILLISECONDS);
  }

  @Override
  public @Nullable WorldPosition convertPosition(@NonNull ServerLocation location) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry == null) {
      return null;
    }

    return new WorldPosition(
      location.x(),
      location.y(),
      location.z(),
      0,
      0,
      location.world().key().asString(),
      entry.targetGroup());
  }

  @Override
  protected @NonNull PlatformSign<ServerPlayer, Component> createPlatformSign(@NonNull Sign base) {
    return new SpongePlatformSign(base);
  }
}
