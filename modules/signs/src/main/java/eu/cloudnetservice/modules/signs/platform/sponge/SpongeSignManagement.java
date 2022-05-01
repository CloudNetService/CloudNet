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

import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

public class SpongeSignManagement extends PlatformSignManagement<org.spongepowered.api.block.entity.Sign> {

  protected final PluginContainer plugin;
  protected final TaskExecutorService syncExecutor;

  public SpongeSignManagement(@NonNull PluginContainer plugin) {
    this.plugin = plugin;
    this.syncExecutor = Sponge.server().scheduler().executor(plugin);
  }

  @Override
  protected void pushUpdates(@NonNull Sign[] signs, @NonNull SignLayout layout) {
    if (Sponge.server().onMainThread()) {
      this.pushUpdates0(signs, layout);
    } else {
      this.syncExecutor.execute(() -> this.pushUpdates0(signs, layout));
    }
  }

  protected void pushUpdates0(@NonNull Sign[] signs, @NonNull SignLayout layout) {
    for (var sign : signs) {
      this.pushUpdate(sign, layout);
    }
  }

  @Override
  protected void pushUpdate(@NonNull Sign sign, @NonNull SignLayout layout) {
    if (Sponge.server().onMainThread()) {
      this.pushUpdate0(sign, layout);
    } else {
      this.syncExecutor.execute(() -> this.pushUpdate0(sign, layout));
    }
  }

  protected void pushUpdate0(@NonNull Sign sign, @NonNull SignLayout layout) {
    var location = this.locationFromWorldPosition(sign.location());
    if (location != null) {
      // no need if the chunk is loaded - the tile entity is not available if the chunk is unloaded
      var entity = location.blockEntity().orElse(null);
      if (entity instanceof org.spongepowered.api.block.entity.Sign tileSign) {
        var lines = this.replaceLines(sign, layout);
        if (lines != null) {
          for (var i = 0; i < 4; i++) {
            tileSign.lines().set(i, AdventureSerializerUtil.serialize(lines.get(i)));
          }

          this.changeBlock(entity, layout);
        }
      }
    }
  }

  protected void changeBlock(@NonNull BlockEntity entity, @NonNull SignLayout layout) {
    var type = Sponge.game()
      .registry(RegistryTypes.BLOCK_TYPE)
      .findValue(ResourceKey.resolve(layout.blockMaterial()))
      .orElse(null);
    var direction = entity.get(Keys.DIRECTION).orElse(null);
    if (type != null && direction != null) {
      entity.serverLocation().relativeToBlock(direction).setBlockType(type);
    }
  }

  @Override
  public @Nullable Sign signAt(@NonNull org.spongepowered.api.block.entity.Sign sign, @NonNull String group) {
    return this.signAt(this.locationToWorldPosition(sign.serverLocation(), group));
  }

  @Override
  public @Nullable Sign createSign(@NonNull org.spongepowered.api.block.entity.Sign sign, @NonNull String group) {
    return this.createSign(sign, group, null);
  }

  @Override
  public @Nullable Sign createSign(
    @NonNull org.spongepowered.api.block.entity.Sign sign,
    @NonNull String group,
    @Nullable String templatePath
  ) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry != null) {
      var created = new Sign(
        group,
        templatePath,
        this.locationToWorldPosition(sign.serverLocation(), entry.targetGroup()));
      this.createSign(created);
      return created;
    }
    return null;
  }

  @Override
  public void deleteSign(@NonNull org.spongepowered.api.block.entity.Sign sign, @NonNull String group) {
    this.deleteSign(this.locationToWorldPosition(sign.serverLocation(), group));
  }

  @Override
  public int removeMissingSigns() {
    var removed = 0;
    for (var position : this.signs.keySet()) {
      var location = this.locationFromWorldPosition(position);
      if (location != null) {
        var entity = location.blockEntity().orElse(null);
        if (!(entity instanceof org.spongepowered.api.block.entity.Sign)) {
          this.deleteSign(position);
          removed++;
        }
      }
    }
    return removed;
  }

  @Override
  protected void startKnockbackTask() {
    this.syncExecutor.scheduleAtFixedRate(() -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          for (var position : this.signs.keySet()) {
            var location = this.locationFromWorldPosition(position);
            // we check if the chunk at the position is loaded, but we skip empty chunks as they are not yet loaded
            if (location != null && location.world().isChunkLoaded(location.chunkPosition(), false)) {
              var distance = conf.distance();
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
    }, 0, 5 * 50, TimeUnit.MILLISECONDS);
  }

  protected @NonNull WorldPosition locationToWorldPosition(
    @NonNull Location<ServerWorld, ?> location,
    @NonNull String group
  ) {
    return new WorldPosition(location.x(), location.y(), location.z(), 0, 0, location.world().key().formatted(), group);
  }

  protected @Nullable Location<ServerWorld, ?> locationFromWorldPosition(@NonNull WorldPosition position) {
    return Sponge.server().worldManager().world(ResourceKey.resolve(position.world()))
      .map(world -> ServerLocation.of(world, position.x(), position.y(), position.z()))
      .orElse(null);
  }
}
