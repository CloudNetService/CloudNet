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

package eu.cloudnetservice.cloudnet.ext.signs.platform.sponge;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.platform.AbstractPlatformSignManagement;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

public class SpongeSignManagement extends AbstractPlatformSignManagement<org.spongepowered.api.block.entity.Sign> {

  protected final PluginContainer plugin;
  protected final TaskExecutorService syncExecutor;

  public SpongeSignManagement(@NotNull PluginContainer plugin) {
    this.plugin = plugin;
    this.syncExecutor = Sponge.server().scheduler().executor(plugin);
  }

  public static SpongeSignManagement getDefaultInstance() {
    return (SpongeSignManagement) CloudNetDriver.getInstance().getServicesRegistry()
      .getFirstService(SignManagement.class);
  }

  @Override
  protected void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
    if (Sponge.server().onMainThread()) {
      this.pushUpdates0(signs, layout);
    } else {
      this.syncExecutor.execute(() -> this.pushUpdates0(signs, layout));
    }
  }

  protected void pushUpdates0(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
    for (Sign sign : signs) {
      this.pushUpdate(sign, layout);
    }
  }

  @Override
  protected void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout) {
    if (Sponge.server().onMainThread()) {
      this.pushUpdate0(sign, layout);
    } else {
      this.syncExecutor.execute(() -> this.pushUpdate0(sign, layout));
    }
  }

  protected void pushUpdate0(@NotNull Sign sign, @NotNull SignLayout layout) {
    Location<ServerWorld, ?> location = this.locationFromWorldPosition(sign.getLocation());
    if (location != null) {
      // no need if the chunk is loaded - the tile entity is not available if the chunk is unloaded
      BlockEntity entity = location.blockEntity().orElse(null);
      if (entity instanceof org.spongepowered.api.block.entity.Sign) {
        org.spongepowered.api.block.entity.Sign tileSign = (org.spongepowered.api.block.entity.Sign) entity;

        String[] lines = this.replaceLines(sign, layout);
        if (lines != null) {
          for (int i = 0; i < 4; i++) {
            tileSign.lines().set(i, AdventureSerializerUtil.serialize(lines[i]));
          }

          this.changeBlock(entity, layout);
        }
      }
    }
  }

  protected void changeBlock(@NotNull BlockEntity entity, @NotNull SignLayout layout) {
    BlockType type = layout.getBlockMaterial() == null ? null : Sponge.game()
      .registry(RegistryTypes.BLOCK_TYPE)
      .findValue(ResourceKey.resolve(layout.getBlockMaterial()))
      .orElse(null);
    Direction direction = entity.get(Keys.DIRECTION).orElse(null);
    if (type != null && direction != null) {
      entity.serverLocation().relativeToBlock(direction).setBlockType(type);
    }
  }

  @Override
  public @Nullable Sign getSignAt(@NotNull org.spongepowered.api.block.entity.Sign sign) {
    return this.getSignAt(this.locationToWorldPosition(sign.serverLocation()));
  }

  @Override
  public @Nullable Sign createSign(@NotNull org.spongepowered.api.block.entity.Sign sign, @NotNull String group) {
    return this.createSign(sign, group, null);
  }

  @Override
  public @Nullable Sign createSign(
    @NotNull org.spongepowered.api.block.entity.Sign sign,
    @NotNull String group,
    @Nullable String templatePath
  ) {
    SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
    if (entry != null) {
      Sign created = new Sign(
        group,
        templatePath,
        this.locationToWorldPosition(sign.serverLocation(), entry.getTargetGroup()));
      this.createSign(created);
      return created;
    }
    return null;
  }

  @Override
  public void deleteSign(@NotNull org.spongepowered.api.block.entity.Sign sign) {
    this.deleteSign(this.locationToWorldPosition(sign.serverLocation()));
  }

  @Override
  public int removeMissingSigns() {
    int removed = 0;
    for (WorldPosition position : this.signs.keySet()) {
      Location<ServerWorld, ?> location = this.locationFromWorldPosition(position);
      if (location != null) {
        BlockEntity entity = location.blockEntity().orElse(null);
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
      SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
      if (entry != null) {
        SignConfigurationEntry.KnockbackConfiguration configuration = entry.getKnockbackConfiguration();
        if (configuration.isValidAndEnabled()) {
          double distance = configuration.getDistance();

          for (WorldPosition position : this.signs.keySet()) {
            Location<ServerWorld, ?> location = this.locationFromWorldPosition(position);
            if (location != null) {
              for (Entity entity : location.world().nearbyEntities(location.position(), distance)) {
                if (entity instanceof ServerPlayer && (configuration.getBypassPermission() == null
                  || !((ServerPlayer) entity).hasPermission(configuration.getBypassPermission()))) {
                  Vector3d vector = entity.location()
                    .position()
                    .sub(location.position())
                    .normalize()
                    .mul(configuration.getStrength());
                  entity.velocity().set(new Vector3d(vector.x(), 0.2D, vector.z()));
                }
              }
            }
          }
        }
      }
    }, 0, 5 * 50, TimeUnit.MILLISECONDS);
  }

  protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location<ServerWorld, ?> location) {
    return new WorldPosition(location.x(), location.y(), location.z(), 0, 0, location.world().key().formatted(), null);
  }

  protected @NotNull WorldPosition locationToWorldPosition(
    @NotNull Location<ServerWorld, ?> location,
    @NotNull String group
  ) {
    return new WorldPosition(location.x(), location.y(), location.z(), 0, 0, location.world().key().formatted(), group);
  }

  protected @Nullable Location<ServerWorld, ?> locationFromWorldPosition(@NotNull WorldPosition position) {
    return Sponge.server().worldManager().world(ResourceKey.resolve(position.getWorld()))
      .map(world -> ServerLocation.of(world, position.getX(), position.getY(), position.getZ()))
      .orElse(null);
  }
}
