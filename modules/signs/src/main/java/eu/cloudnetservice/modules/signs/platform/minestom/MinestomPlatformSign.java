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

import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import eu.cloudnetservice.ext.adventure.AdventureTextFormatLookup;
import eu.cloudnetservice.modules.bridge.platform.minestom.MinestomInstanceProvider;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.minestom.event.MinestomCloudSignInteractEvent;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;

public class MinestomPlatformSign extends PlatformSign<Player, String> {

  private final MinestomInstanceProvider provider;
  private Pair<Pos, Instance> signLocation;

  public MinestomPlatformSign(
    @NonNull Sign base,
    @NonNull MinestomInstanceProvider provider
  ) {
    super(base, input -> {
      var coloredComponent = AdventureSerializerUtil.serialize(input);
      return GsonComponentSerializer.gson().serialize(coloredComponent);
    });
    this.provider = provider;
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
    EventDispatcher.call(event);

    return event.isCancelled() ? null : event.target();
  }

  public @Nullable Pair<Pos, Instance> signLocation() {
    var instance = this.provider.instanceByIdentifier(this.base.location().world());
    if (this.signLocation != null) {
      return this.signLocation;
    }
    // check if the instance of the sign is available
    if (instance == null) {
      return null;
    }
    var worldPos = this.base.location();
    return this.signLocation = new Pair<>(new Pos(worldPos.x(), worldPos.y(), worldPos.z()), instance);
  }
}
