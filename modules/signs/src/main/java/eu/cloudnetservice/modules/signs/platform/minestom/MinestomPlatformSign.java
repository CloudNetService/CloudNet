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

package eu.cloudnetservice.modules.signs.platform.minestom;

import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.adventure.AdventureTextFormatLookup;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.minestom.event.MinestomCloudSignInteractEvent;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;

public class MinestomPlatformSign extends PlatformSign<Player, String> {

  private final GlobalEventHandler eventHandler;
  private final InstanceManager instanceManager;

  private Tuple2<Pos, Instance> signLocation;

  public MinestomPlatformSign(
    @NonNull Sign base,
    @NonNull GlobalEventHandler eventHandler,
    @NonNull InstanceManager instanceManager
  ) {
    super(base, input -> {
      var coloredComponent = ComponentFormats.BUNGEE_TO_ADVENTURE.convert(input);
      return GsonComponentSerializer.gson().serialize(coloredComponent);
    });

    this.eventHandler = eventHandler;
    this.instanceManager = instanceManager;
  }

  @Override
  public boolean exists() {
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    return location.second().getBlock(location.first()).name().contains("sign");
  }

  @Override
  public boolean needsUpdates() {
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    var chunkX = location.first().chunkX();
    var chunkZ = location.first().chunkZ();

    return location.second().isChunkLoaded(chunkX, chunkZ);
  }

  @Override
  public void updateSign(@NonNull SignLayout layout) {
    var location = this.signLocation();
    if (location != null) {
      // build the compound information about the sign
      var compound = NBT.Compound(nbt -> {
        // set the sign glowing if requested
        var glowingColor = layout.glowingColor();
        if (glowingColor != null && glowingColor.length() == 1) {
          var color = AdventureTextFormatLookup.findColor(glowingColor.charAt(0));

          nbt.put("GlowingText", NBT.Boolean(color != null));
          nbt.put("Color", NBT.String(color == null ? NamedTextColor.WHITE.toString() : color.toString()));
        }

        // set the sign lines
        this.changeSignLines(layout, (index, line) -> nbt.put("Text" + (index + 1), NBT.String(line)));
      });

      // set the block at the position
      var block = location.second().getBlock(location.first());
      location.second().setBlock(
        location.first(),
        block.withHandler(MinestomSignBlockHandler.SIGN_BLOCK_HANDLER).withNbt(compound));
    }
  }

  @Override
  public @Nullable ServiceInfoSnapshot callSignInteractEvent(@NonNull Player player) {
    var event = new MinestomCloudSignInteractEvent(player, this);
    this.eventHandler.call(event);

    return event.isCancelled() ? null : event.target();
  }

  public @Nullable Tuple2<Pos, Instance> signLocation() {
    // lazy init - if we have one use it
    if (this.signLocation != null) {
      return this.signLocation;
    }

    var instance = this.instanceManager.getInstance(UUID.fromString(this.base.location().world()));
    // check if the instance of the sign is available
    if (instance == null) {
      return null;
    }

    var worldPos = this.base.location();
    return this.signLocation = new Tuple2<>(new Pos(worldPos.x(), worldPos.y(), worldPos.z()), instance);
  }
}
