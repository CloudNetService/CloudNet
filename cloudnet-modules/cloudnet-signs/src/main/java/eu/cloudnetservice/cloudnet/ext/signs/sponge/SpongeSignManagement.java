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

package eu.cloudnetservice.cloudnet.ext.signs.sponge;

import com.flowpowered.math.vector.Vector3d;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.block.DirectionalData;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SpongeSignManagement extends AbstractServiceSignManagement<org.spongepowered.api.block.tileentity.Sign> {

  protected final Object plugin;

  public SpongeSignManagement(Object plugin) {
    this.plugin = plugin;
  }

  public static SpongeSignManagement getDefaultInstance() {
    return (SpongeSignManagement) CloudNetDriver.getInstance().getServicesRegistry()
      .getFirstService(SignManagement.class);
  }

  @Override
  protected void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
    if (Sponge.getServer().isMainThread()) {
      this.pushUpdates0(signs, layout);
    } else {
      Sponge.getGame().getScheduler()
        .createTaskBuilder()
        .execute(() -> this.pushUpdates0(signs, layout))
        .submit(this.plugin);
    }
  }

  protected void pushUpdates0(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
    for (Sign sign : signs) {
      this.pushUpdate(sign, layout);
    }
  }

  @Override
  protected void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout) {
    if (Sponge.getServer().isMainThread()) {
      this.pushUpdate0(sign, layout);
    } else {
      Sponge.getGame().getScheduler()
        .createTaskBuilder()
        .execute(() -> this.pushUpdate0(sign, layout))
        .submit(this.plugin);
    }
  }

  protected void pushUpdate0(@NotNull Sign sign, @NotNull SignLayout layout) {
    Location<World> location = this.locationFromWorldPosition(sign.getWorldPosition());
    if (location != null) {
      // no need if the chunk is loaded - the tile entity is not available if the chunk is unloaded
      TileEntity entity = location.getTileEntity().orElse(null);
      if (entity instanceof org.spongepowered.api.block.tileentity.Sign) {
        org.spongepowered.api.block.tileentity.Sign tileSign = (org.spongepowered.api.block.tileentity.Sign) entity;
        SignData signData = tileSign.getSignData();

        String[] lines = this.replaceLines(sign, layout);
        if (lines != null) {
          for (int i = 0; i < 4; i++) {
            signData.setElement(i, TextSerializers.FORMATTING_CODE.deserialize(lines[i]));
          }

          tileSign.offer(signData);
          this.changeBlock(entity, layout);
        }
      }
    }
  }

  protected void changeBlock(TileEntity entity, SignLayout layout) {
    BlockType type = layout.getBlockMaterial() == null ? null : Sponge.getGame().getRegistry().getType(
      BlockType.class, layout.getBlockMaterial()).orElse(null);
    DirectionalData directionalData = entity.getLocation().get(DirectionalData.class).orElse(null);
    if (type != null && directionalData != null) {
      directionalData.get(Keys.DIRECTION).ifPresent(direction -> {
        Location<World> relativeBlock = entity.getLocation().getBlockRelative(direction.getOpposite());
        relativeBlock.setBlockType(type);
      });
    }
  }

  @Override
  public @Nullable Sign getSignAt(org.spongepowered.api.block.tileentity.@NotNull Sign sign) {
    return this.getSignAt(this.locationToWorldPosition(sign.getLocation()));
  }

  @Override
  public @Nullable Sign createSign(org.spongepowered.api.block.tileentity.@NotNull Sign sign, @NotNull String group) {
    return this.createSign(sign, group, null);
  }

  @Override
  public @Nullable Sign createSign(org.spongepowered.api.block.tileentity.@NotNull Sign sign, @NotNull String group,
    @Nullable String templatePath) {
    SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
    if (entry != null) {
      Sign created = new Sign(group, entry.getTargetGroup(), templatePath,
        this.locationToWorldPosition(sign.getLocation(), entry.getTargetGroup()));
      this.createSign(created);
      return created;
    }
    return null;
  }

  @Override
  public void deleteSign(org.spongepowered.api.block.tileentity.@NotNull Sign sign) {
    this.deleteSign(this.locationToWorldPosition(sign.getLocation()));
  }

  @Override
  public int removeMissingSigns() {
    int removed = 0;
    for (WorldPosition position : this.signs.keySet()) {
      Location<World> location = this.locationFromWorldPosition(position);
      if (location != null) {
        TileEntity tileEntity = location.getTileEntity().orElse(null);
        if (!(tileEntity instanceof org.spongepowered.api.block.tileentity.Sign)) {
          this.deleteSign(position);
          removed++;
        }
      }
    }
    return removed;
  }

  @Override
  protected void startKnockbackTask() {
    Sponge.getScheduler()
      .createTaskBuilder()
      .execute(() -> {
        SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
        if (entry != null) {
          SignConfigurationEntry.KnockbackConfiguration configuration = entry.getKnockbackConfiguration();
          if (configuration.isValidAndEnabled()) {
            double distance = configuration.getDistance();

            for (WorldPosition position : this.signs.keySet()) {
              Location<World> location = this.locationFromWorldPosition(position);
              if (location != null) {
                for (Entity entity : location.getExtent().getNearbyEntities(location.getPosition(), distance)) {
                  if (entity instanceof Player && (configuration.getBypassPermission() == null
                    || !((Player) entity).hasPermission(configuration.getBypassPermission()))) {
                    Vector3d vector = entity.getLocation()
                      .getPosition()
                      .sub(location.getPosition())
                      .normalize()
                      .mul(configuration.getStrength());
                    entity.setVelocity(new Vector3d(vector.getX(), 0.2D, vector.getZ()));
                  }
                }
              }
            }
          }
        }
      })
      .delayTicks(0)
      .intervalTicks(5)
      .submit(this.plugin);
  }

  protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location<World> location) {
    return new WorldPosition(location.getX(), location.getY(), location.getZ(), 0, 0, location.getExtent().getName());
  }

  protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location<World> location, @NotNull String group) {
    return new WorldPosition(location.getX(), location.getY(), location.getZ(), 0, 0, location.getExtent().getName(),
      group);
  }

  protected @Nullable Location<World> locationFromWorldPosition(@NotNull WorldPosition position) {
    return Sponge.getServer().getWorld(position.getWorld())
      .map(world -> new Location<>(world, position.getX(), position.getY(), position.getZ()))
      .orElse(null);
  }
}
