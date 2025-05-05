/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.impl.platform.sponge;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.impl.platform.sponge.event.SpongeCloudSignInteractEvent;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.WorldManager;

final class SpongePlatformSign extends PlatformSign<ServerPlayer, Component> {

  private final Game game;
  private final EventManager eventManager;
  private final WorldManager worldManager;

  // lazy initialized once available
  private ServerLocation signLocation;

  public SpongePlatformSign(
    @NonNull Sign base,
    @NonNull Game game,
    @NonNull EventManager eventManager,
    @NonNull WorldManager worldManager,
    @NonNull ServiceRegistry serviceRegistry
  ) {
    super(base, serviceRegistry, ComponentFormats.BUNGEE_TO_ADVENTURE::convert);

    this.game = game;
    this.eventManager = eventManager;
    this.worldManager = worldManager;
  }

  @Override
  public boolean exists() {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    // get the block at the given location
    var block = location.blockEntity().orElse(null);
    return block instanceof org.spongepowered.api.block.entity.Sign;
  }

  @Override
  public boolean needsUpdates() {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    return location.isAvailable() && location.world().isChunkLoaded(location.chunkPosition(), false);
  }

  @Override
  public void updateSign(@NonNull SignLayout layout) {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return;
    }

    // get the block at the given location
    var entity = location.blockEntity().orElse(null);
    if (entity instanceof org.spongepowered.api.block.entity.Sign sign) {
      // set the glowing status if needed
      sign.glowingText().set(layout.glowingColor() != null);

      // set the sign lines
      List<Component> lines = new ArrayList<>(4);
      this.changeSignLines(layout, lines::add);
      sign.offer(Keys.SIGN_LINES, lines);

      // change the block behind the sign
      var type = this.game
        .registry(RegistryTypes.BLOCK_TYPE)
        .findValue(ResourceKey.resolve(layout.blockMaterial()))
        .orElse(null);
      var direction = entity.get(Keys.DIRECTION).orElse(null);
      if (type != null && direction != null) {
        entity.serverLocation().relativeToBlock(direction.opposite()).setBlockType(type);
      }
    }
  }

  @Override
  public @Nullable ServiceInfoSnapshot callSignInteractEvent(@NonNull ServerPlayer player) {
    var event = new SpongeCloudSignInteractEvent(
      Cause.of(
        EventContext.builder().add(EventContextKeys.PLAYER, player).build(),
        player),
      player,
      this);
    return this.eventManager.post(event) ? null : event.target();
  }

  public @Nullable ServerLocation signLocation() {
    // check if the location is already available
    if (this.signLocation != null) {
      return this.signLocation;
    }

    // check if the world associated with the sign is available
    var loc = this.base.location();
    var world = this.worldManager.world(ResourceKey.resolve(loc.world())).orElse(null);
    if (world == null) {
      return null;
    }

    // initialize and return the location
    return this.signLocation = ServerLocation.of(world, loc.x(), loc.y(), loc.z());
  }
}
