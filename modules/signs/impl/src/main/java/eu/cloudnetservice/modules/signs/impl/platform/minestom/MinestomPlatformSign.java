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

package eu.cloudnetservice.modules.signs.impl.platform.minestom;

import com.google.common.base.Enums;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.impl.platform.minestom.event.MinestomCloudSignInteractEvent;
import eu.cloudnetservice.utils.base.StringUtil;
import io.vavr.Tuple2;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.color.DyeColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import org.jetbrains.annotations.Nullable;

public class MinestomPlatformSign extends PlatformSign<Player, String> {

  private final GlobalEventHandler eventHandler;
  private final InstanceManager instanceManager;

  private Tuple2<Pos, Instance> signLocation;

  public MinestomPlatformSign(
    @NonNull Sign base,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull GlobalEventHandler eventHandler,
    @NonNull InstanceManager instanceManager
  ) {
    super(base, serviceRegistry, input -> {
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

    return location._2().getBlock(location._1()).name().contains("sign");
  }

  @Override
  public boolean needsUpdates() {
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    var chunkX = location._1().chunkX();
    var chunkZ = location._1().chunkZ();

    return location._2().isChunkLoaded(chunkX, chunkZ);
  }

  @Override
  public void updateSign(@NonNull SignLayout layout) {
    var location = this.signLocation();
    if (location != null) {
      // set the glowing state
      var textCompound = CompoundBinaryTag.builder();
      textCompound.putBoolean("has_glowing_text", layout.textGlowing());

      // set the text color
      var textColor = layout.textColor();
      var dyeColor = switch (textColor) {
        case String string -> Enums.getIfPresent(DyeColor.class, StringUtil.toUpper(string)).or(DyeColor.BLACK);
        case null -> DyeColor.BLACK;
      };
      var serializedColor = DyeColor.NBT_TYPE.write(dyeColor);
      textCompound.put("color", serializedColor);

      // set the sign lines - they are provided as legacy text components and need to be converted to JSON
      var linesCompound = ListBinaryTag.builder();
      this.changeSignLines(layout, (_, line) -> linesCompound.add(StringBinaryTag.stringBinaryTag(line)));
      textCompound.put("messages", linesCompound.build());

      // build the final sign compound
      var signCompound = CompoundBinaryTag.builder();
      signCompound.putBoolean("is_waxed", false);
      signCompound.put("front_text", textCompound.build());
      signCompound.put("back_text", textCompound.build());

      // set the block at the position
      var block = location._2().getBlock(location._1());
      location._2().setBlock(
        location._1(),
        block.withHandler(MinestomSignBlockHandler.SIGN_BLOCK_HANDLER).withNbt(signCompound.build()));
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
